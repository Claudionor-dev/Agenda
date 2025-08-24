const container = document.getElementById('container');

async function fetchAllDates() {
    // Pega todos os arquivos XML da pasta agenda lendo /agenda?data=yyyy-MM-dd
    // Como o backend não lista arquivos, vamos criar uma lista fixa por enquanto
    // Ou você pode implementar uma rota para listar datas (sugestão futura)

    // Por enquanto, teste com estas datas (adicione os XMLs na pasta agenda)
    return ['2025-08-21', '2025-08-22', '2025-08-23'];
}

async function loadData(data) {
    const res = await fetch(`/agenda?data=${data}`);
    if (!res.ok) {
        alert(`Erro ao carregar agenda para ${data}`);
        return null;
    }
    return await res.json();
}

function createInput(labelText, name, value = '', placeholder = '', type = 'text') {
    const label = document.createElement('label');
    label.textContent = labelText;
    const input = document.createElement(type === 'textarea' ? 'textarea' : 'input');
    input.name = name;
    input.value = value;
    input.placeholder = placeholder;
    if (type !== 'textarea') input.type = type;
    label.appendChild(document.createElement('br'));
    label.appendChild(input);
    return label;
}

function createButton(text, onClick) {
    const btn = document.createElement('button');
    btn.textContent = text;
    btn.type = 'button';
    btn.onclick = onClick;
    return btn;
}

function formatDateBR(data) {
    const d = new Date(data);
    return d.toLocaleDateString('pt-BR');
}

function createItemElement(item, parentSaveCallback) {
    const div = document.createElement('div');
    div.classList.add('item');
    div.dataset.id = item.id;

    const header = document.createElement('div');
    header.classList.add('item-header');

    const titleSpan = document.createElement('span');
    titleSpan.textContent = item.titulo;
    header.appendChild(titleSpan);

    // Botão abrir subareas
    const btnToggleSubs = createButton('Subáreas', () => {
        const subDiv = div.querySelector('.subareas');
        if (subDiv.classList.contains('hidden')) {
            subDiv.classList.remove('hidden');
        } else {
            subDiv.classList.add('hidden');
        }
    });
    header.appendChild(btnToggleSubs);

    // Botões editar e remover
    const actions = document.createElement('div');
    actions.classList.add('actions');

    const btnEdit = createButton('Editar', () => editItem(div, parentSaveCallback));
    const btnRemove = createButton('Remover', () => removeItem(div, parentSaveCallback));

    actions.appendChild(btnEdit);
    actions.appendChild(btnRemove);
    header.appendChild(actions);

    div.appendChild(header);

    // Exibe hora e local (opcional)
    const info = document.createElement('div');
    info.textContent = (item.hora ? `Hora: ${item.hora}` : '') + (item.local ? ` | Local: ${item.local}` : '');
    div.appendChild(info);

    // Descrição
    if (item.descricao) {
        const desc = document.createElement('div');
        desc.textContent = item.descricao;
        desc.style.fontStyle = 'italic';
        div.appendChild(desc);
    }

    // Subareas
    const subareasDiv = document.createElement('div');
    subareasDiv.classList.add('subareas', 'hidden');

    // Botão para adicionar subarea
    const btnAddSub = createButton('Adicionar Subárea', () => addSubarea(item, subareasDiv, parentSaveCallback));
    subareasDiv.appendChild(btnAddSub);

    for (const sub of item.subareas || []) {
        const subDiv = createSubareaElement(sub, item, parentSaveCallback);
        subareasDiv.appendChild(subDiv);
    }

    div.appendChild(subareasDiv);

    return div;
}

function createSubareaElement(sub, parentItem, parentSaveCallback) {
    const div = document.createElement('div');
    div.classList.add('subarea');
    div.dataset.id = sub.id;

    const titleSpan = document.createElement('span');
    titleSpan.textContent = sub.titulo;
    div.appendChild(titleSpan);

    const actions = document.createElement('div');
    actions.style.display = 'flex';

    const btnEdit = createButton('Editar', () => editSubarea(div, parentItem, parentSaveCallback));
    const btnRemove = createButton('Remover', () => removeSubarea(div, parentItem, parentSaveCallback));
    actions.appendChild(btnEdit);
    actions.appendChild(btnRemove);

    div.appendChild(actions);

    return div;
}

function editItem(div, saveCallback) {
    const id = div.dataset.id;
    const titulo = prompt("Editar título:", div.querySelector('.item-header span').textContent);
    if (!titulo) return alert("Título obrigatório");

    const hora = prompt("Editar hora (opcional):", "");
    const local = prompt("Editar local (opcional):", "");
    const descricao = prompt("Editar descrição (opcional):", "");

    // Atualiza no objeto em memória
    // Para simplificar, chama o saveCallback para recarregar (no projeto real faria edição local)
    saveCallback(id, {
        id, titulo, hora, local, descricao
    });
}

function removeItem(div, saveCallback) {
    if (!confirm("Confirma remoção do item?")) return;
    saveCallback(div.dataset.id, null);
}

function addSubarea(item, subareasDiv, saveCallback) {
    const titulo = prompt("Título da subárea:");
    if (!titulo) return alert("Título obrigatório");

    const hora = prompt("Hora (opcional):", "");
    const descricao = prompt("Descrição (opcional):", "");

    if (!item.subareas) item.subareas = [];
    item.subareas.push({
        id: crypto.randomUUID(),
        titulo,
        hora,
        descricao
    });

    saveCallback(item.id, item);
}

function editSubarea(div, parentItem, saveCallback) {
    const id = div.dataset.id;
    const sub = parentItem.subareas.find(s => s.id === id);
    if (!sub) return alert("Subárea não encontrada");

    const titulo = prompt("Editar título da subárea:", sub.titulo);
    if (!titulo) return alert("Título obrigatório");

    const hora = prompt("Editar hora (opcional):", sub.hora || "");
    const descricao = prompt("Editar descrição (opcional):", sub.descricao || "");

    sub.titulo = titulo;
    sub.hora = hora;
    sub.descricao = descricao;

    saveCallback(parentItem.id, parentItem);
}

function removeSubarea(div, parentItem, saveCallback) {
    if (!confirm("Confirma remoção da subárea?")) return;
    const id = div.dataset.id;
    parentItem.subareas = parentItem.subareas.filter(s => s.id !== id);
    saveCallback(parentItem.id, parentItem);
}

function createDayColumn(data, items) {
    const div = document.createElement('div');
    div.classList.add('dia');

    const title = document.createElement('h2');
    title.textContent = formatDateBR(data);
    div.appendChild(title);

    // Form adicionar novo item
    const form = document.createElement('form');

    const inputTitulo = createInput('Título *', 'titulo');
    const inputHora = createInput('Hora', 'hora');
    const inputLocal = createInput('Local', 'local');
    const inputDesc = createInput('Descrição', 'descricao', '', '', 'textarea');

    form.appendChild(inputTitulo);
    form.appendChild(inputHora);
    form.appendChild(inputLocal);
    form.appendChild(inputDesc);

    const btnAdd = createButton('Adicionar', async () => {
        const tituloVal = inputTitulo.querySelector('input,textarea').value.trim();
        if (!tituloVal) return alert('Título é obrigatório');

        const horaVal = inputHora.querySelector('input').value.trim();
        const localVal = inputLocal.querySelector('input').value.trim();
        const descVal = inputDesc.querySelector('textarea').value.trim();

        // POST item novo
        const newItem = {
            titulo: tituloVal,
            hora: horaVal,
            local: localVal,
            descricao: descVal
        };

        const res = await fetch(`/agenda?data=${data}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(newItem)
        });
        if (res.ok) {
            loadAndRender(); // recarrega tudo
        } else {
            alert('Erro ao adicionar item');
        }
    });
    form.appendChild(btnAdd);

    div.appendChild(form);

    // Lista de itens
    const listDiv = document.createElement('div');
    listDiv.classList.add('lista-itens');

    for (const item of items) {
        const itemEl = createItemElement(item, async (id, updatedItem) => {
            if (updatedItem === null) {
                // remover
                const delBody = { id };
                // se for subarea? tratar depois
                const res = await fetch(`/agenda?data=${data}`, {
                    method: 'DELETE',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(delBody)
                });
                if (res.ok) {
                    loadAndRender();
                } else {
                    alert('Erro ao remover item');
                }
            } else {
                // editar ou atualizar
                const putBody = updatedItem;
                const res = await fetch(`/agenda?data=${data}`, {
                    method: 'PUT',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(putBody)
                });
                if (res.ok) {
                    loadAndRender();
                } else {
                    alert('Erro ao atualizar item');
                }
            }
        });
        listDiv.appendChild(itemEl);
    }
    div.appendChild(listDiv);

    // Botão salvar individual - neste sistema o salvar já ocorre direto, mas deixo para eventual uso
    /*
    const btnSave = createButton('Salvar', () => {
        // Opcional: implementar salvar tudo da coluna
    });
    div.appendChild(btnSave);
    */

    return div;
}

async function loadAndRender() {
    container.innerHTML = '';
    const datas = await fetchAllDates();

    for (const data of datas) {
        const agendaData = await loadData(data);
        if (!agendaData) continue;

        const col = createDayColumn(agendaData.data, agendaData.items);
        container.appendChild(col);
    }
}

loadAndRender();
