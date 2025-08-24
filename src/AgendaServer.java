import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AgendaServer {
    private static final String AGENDA_DIR = "agenda";

    public static void main(String[] args) throws Exception {
        // Cria pasta agenda se não existir
        File dir = new File(AGENDA_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/agenda", new AgendaHandler());
        server.createContext("/", new StaticHandler());

        System.out.println("Servidor rodando em http://localhost:8000");
        server.start();
    }

    static class AgendaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery(); // ex: data=2025-08-21
            String data = getParam(query, "data");

            if (data == null || data.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Parâmetro 'data' obrigatório\"}");
                return;
            }

            File xmlFile = new File(AGENDA_DIR + "/" + data + ".xml");

            switch (method) {
                case "GET":
                    if (!xmlFile.exists()) {
                        // cria arquivo vazio
                        createEmptyXML(xmlFile, data);
                    }
                    JSONArray items = readXml(xmlFile);
                    JSONObject resposta = new JSONObject();
                    resposta.put("data", data);
                    resposta.put("items", items);
                    sendResponse(exchange, 200, resposta.toString());
                    break;
                case "POST":
                    // adiciona item
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(body);
                    if (!xmlFile.exists()) createEmptyXML(xmlFile, data);
                    addItem(xmlFile, json);
                    sendResponse(exchange, 200, "{\"status\":\"ok\"}");
                    break;
                case "PUT":
                    // atualiza item
                    String bodyPut = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject jsonPut = new JSONObject(bodyPut);
                    if (!xmlFile.exists()) {
                        sendResponse(exchange, 404, "{\"error\":\"Data não encontrada\"}");
                        return;
                    }
                    updateItem(xmlFile, jsonPut);
                    sendResponse(exchange, 200, "{\"status\":\"ok\"}");
                    break;
                case "DELETE":
                    // remove item ou subitem
                    String bodyDel = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject jsonDel = new JSONObject(bodyDel);
                    if (!xmlFile.exists()) {
                        sendResponse(exchange, 404, "{\"error\":\"Data não encontrada\"}");
                        return;
                    }
                    deleteItem(xmlFile, jsonDel);
                    sendResponse(exchange, 200, "{\"status\":\"ok\"}");
                    break;
                default:
                    sendResponse(exchange, 405, "{\"error\":\"Método não permitido\"}");
            }
        }

        private String getParam(String query, String key) {
            if (query == null) return null;
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2 && parts[0].equals(key)) return parts[1];
            }
            return null;
        }

        private void createEmptyXML(File file, String data) throws IOException {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element root = doc.createElement("agenda");
                root.setAttribute("data", data);
                doc.appendChild(root);

                saveDocument(doc, file);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Erro ao criar XML");
            }
        }

        private JSONArray readXml(File file) {
            JSONArray array = new JSONArray();
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);

                Element root = doc.getDocumentElement();

                NodeList items = root.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element el = (Element) items.item(i);
                    JSONObject obj = new JSONObject();
                    obj.put("id", el.getAttribute("id"));
                    obj.put("titulo", getTagValue(el, "titulo"));
                    obj.put("hora", getTagValue(el, "hora"));
                    obj.put("local", getTagValue(el, "local"));
                    obj.put("descricao", getTagValue(el, "descricao"));

                    // lê subareas
                    JSONArray subs = new JSONArray();
                    NodeList subareasList = el.getElementsByTagName("subarea");
                    for (int j = 0; j < subareasList.getLength(); j++) {
                        Element subEl = (Element) subareasList.item(j);
                        JSONObject subObj = new JSONObject();
                        subObj.put("id", subEl.getAttribute("id"));
                        subObj.put("titulo", getTagValue(subEl, "titulo"));
                        subObj.put("hora", getTagValue(subEl, "hora"));
                        subObj.put("descricao", getTagValue(subEl, "descricao"));
                        subs.put(subObj);
                    }
                    obj.put("subareas", subs);
                    array.put(obj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return array;
        }

        private String getTagValue(Element el, String tag) {
            NodeList nl = el.getElementsByTagName(tag);
            if (nl.getLength() > 0) {
                return nl.item(0).getTextContent();
            }
            return "";
        }

        private void addItem(File file, JSONObject json) throws IOException {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);

                Element root = doc.getDocumentElement();

                Element item = doc.createElement("item");
                String id = UUID.randomUUID().toString();
                item.setAttribute("id", id);

                Element titulo = doc.createElement("titulo");
                titulo.setTextContent(json.getString("titulo"));
                item.appendChild(titulo);

                Element hora = doc.createElement("hora");
                hora.setTextContent(json.optString("hora", ""));
                item.appendChild(hora);

                Element local = doc.createElement("local");
                local.setTextContent(json.optString("local", ""));
                item.appendChild(local);

                Element descricao = doc.createElement("descricao");
                descricao.setTextContent(json.optString("descricao", ""));
                item.appendChild(descricao);

                Element subareas = doc.createElement("subareas");
                item.appendChild(subareas);

                root.appendChild(item);

                saveDocument(doc, file);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Erro ao adicionar item");
            }
        }

        private void updateItem(File file, JSONObject json) throws IOException {
            try {
                String id = json.getString("id");

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);

                Element root = doc.getDocumentElement();
                NodeList items = root.getElementsByTagName("item");

                for (int i = 0; i < items.getLength(); i++) {
                    Element el = (Element) items.item(i);
                    if (el.getAttribute("id").equals(id)) {
                        setTagValue(doc, el, "titulo", json.getString("titulo"));
                        setTagValue(doc, el, "hora", json.optString("hora", ""));
                        setTagValue(doc, el, "local", json.optString("local", ""));
                        setTagValue(doc, el, "descricao", json.optString("descricao", ""));

                        // atualiza subareas se houver
                        if (json.has("subareas")) {
                            Element subareasEl = (Element) el.getElementsByTagName("subareas").item(0);
                            // limpa subareas atuais
                            while (subareasEl.hasChildNodes()) {
                                subareasEl.removeChild(subareasEl.getFirstChild());
                            }
                            JSONArray subs = json.getJSONArray("subareas");
                            for (int j = 0; j < subs.length(); j++) {
                                JSONObject subObj = subs.getJSONObject(j);
                                Element subEl = doc.createElement("subarea");
                                String subId = subObj.has("id") ? subObj.getString("id") : UUID.randomUUID().toString();
                                subEl.setAttribute("id", subId);

                                Element subTitulo = doc.createElement("titulo");
                                subTitulo.setTextContent(subObj.getString("titulo"));
                                subEl.appendChild(subTitulo);

                                Element subHora = doc.createElement("hora");
                                subHora.setTextContent(subObj.optString("hora", ""));
                                subEl.appendChild(subHora);

                                Element subDesc = doc.createElement("descricao");
                                subDesc.setTextContent(subObj.optString("descricao", ""));
                                subEl.appendChild(subDesc);

                                subareasEl.appendChild(subEl);
                            }
                        }

                        break;
                    }
                }
                saveDocument(doc, file);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Erro ao atualizar item");
            }
        }

        private void deleteItem(File file, JSONObject json) throws IOException {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);

                Element root = doc.getDocumentElement();

                if (json.has("subId")) {
                    // remover subarea
                    String itemId = json.getString("id");
                    String subId = json.getString("subId");

                    NodeList items = root.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element el = (Element) items.item(i);
                        if (el.getAttribute("id").equals(itemId)) {
                            Element subareasEl = (Element) el.getElementsByTagName("subareas").item(0);
                            NodeList subs = subareasEl.getElementsByTagName("subarea");
                            for (int j = 0; j < subs.getLength(); j++) {
                                Element subEl = (Element) subs.item(j);
                                if (subEl.getAttribute("id").equals(subId)) {
                                    subareasEl.removeChild(subEl);
                                    saveDocument(doc, file);
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    // remover item inteiro
                    String id = json.getString("id");
                    NodeList items = root.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element el = (Element) items.item(i);
                        if (el.getAttribute("id").equals(id)) {
                            root.removeChild(el);
                            saveDocument(doc, file);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Erro ao deletar item");
            }
        }

        private void setTagValue(Document doc, Element parent, String tag, String value) {
            NodeList nodes = parent.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                nodes.item(0).setTextContent(value);
            } else {
                Element el = doc.createElement(tag);
                el.setTextContent(value);
                parent.appendChild(el);
            }
        }

        private void saveDocument(Document doc, File file) throws Exception {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }

        // ** O método que faltava — manda resposta com status e texto **
        private static void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("C:\\Users\\claudionor\\Desktop\\Projeto Faculdade\\agenda\\frontend" + path);

            //File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String contentType = guessContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        }

        private String guessContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }
}
