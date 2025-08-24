**Projeto Agenda**

O **Agenda** é um sistema simples de gerenciamento de eventos, que permite ao usuário organizar compromissos pessoais ou profissionais com base em data, hora, descrição, local e título.

Todos os eventos são salvos **localmente** no formato **XML**, organizados dentro de uma pasta chamada `agenda/`.

---

**Funcionalidades**

-  Adicionar novo evento com:
  - Título
  - Data
  - Hora
  - Descrição
  - Local
-  Editar eventos existentes
-  Excluir eventos
-  Visualizar eventos filtrados por data
-  Armazenamento local de eventos em arquivos XML

---

**Estrutura de Armazenamento**

- `frontend/` — camada de interface (HTML, CSS, JavaScript)
- `src/` — código Java do backend
- `.gitignore` — arquivos e pastas ignorados pelo Git
- `json_20250517.xml` — exemplo ou arquivo de teste contendo dados em XML

Os eventos gerados pelo sistema devem ser salvos em arquivos `.xml` dentro da pasta `agenda/`.

---

- Cada evento é representado por um arquivo XML individual, ou
- Todos os eventos podem estar centralizados em um único arquivo XML, dependendo da implementação.

**Exemplo de estrutura:**

/agenda
├── evento_2025-08-24_14-00.xml
├── evento_2025-08-25_09-30.xml
└── ...


**Repositório Oficial**

O projeto está hospedado no GitHub:  
-> [https://github.com/Claudionor-dev/Agenda](https://github.com/Claudionor-dev/Agenda)

---

