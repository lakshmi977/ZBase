
// Redirect from create.html if already logged in
window.onload = async function () {
  if (window.location.pathname.endsWith("create.html")) {
    window.location.href = "dashboard.html"; // Redirect if already logged in
  }
};

// Append currentDatabase (ensure that "currentDatabase" is defined elsewhere)
document.getElementById("selectedDb").textContent += currentDatabase;

// Global variables
let columnCounter = 0;
let columnsArea = document.getElementById("columnsArea");
console.log(columnsArea);
console.log(databases); // "databases" should be defined and populated elsewhere

let generateSqlBtn = document.getElementById("generateSqlBtn");
let sqlPreview = document.getElementById("sqlPreview");
let tableForm = document.getElementById("tableForm");

// Sample data types for dropdown
let dataTypes = ["INT", "STRING", "BOOLEAN", "FLOAT", "BLOB"];

// Sample tables for foreign key references
let sampleTables = ["users", "products", "categories", "orders", "customers"];

document.addEventListener("DOMContentLoaded", function () {
  console.log("DOM fully loaded");

  // Sample columns for foreign key references
  let sampleColumns = {
    users: ["id", "username", "email"],
    products: ["id", "sku", "product_code"],
    categories: ["id", "category_code"],
    orders: ["id", "order_number"],
    customers: ["id", "customer_code"]
  };

  // Remove column function
  window.removeCons = function (columnId) {
    const columnElement = document.getElementById(columnId);
    if (columnElement) {
      columnElement.remove();
    }
  };

  // Toggle default value section
  window.toggleDefaultSection = function (columnId) {
    const checkbox = document.getElementById(`${columnId}-default`);
    const section = document.getElementById(`${columnId}-default-section`);
    section.style.display = checkbox.checked ? "block" : "none";
  };

  // Toggle foreign key section
  window.toggleFKSection = function (columnId) {
    const checkbox = document.getElementById(`${columnId}-fk`);
    const section = document.getElementById(`${columnId}-fk-section`);
    section.style.display = checkbox.checked ? "block" : "none";
  };

  // Update reference columns based on selected table
  window.updateDropColumn = function (columnId) {
    const refTable = document.getElementById(`${columnId}-ref-table`).value;
    const refColumnSelect = document.getElementById(`${columnId}-ref-column`);
    // Clear previous options
    refColumnSelect.innerHTML = '<option value="">Select reference column</option>';
    // Add new options based on selected table if available in sampleColumns
    if (refTable && sampleColumns[refTable]) {
      sampleColumns[refTable].forEach((column) => {
        const option = document.createElement("option");
        option.value = column;
        option.textContent = column;
        refColumnSelect.appendChild(option);
      });
    }
  };

  // Generate SQL statement
  function generateSQL() {
    const tableName = document.getElementById("tableName").value || "table_name";
    let sql = `<span class="sql-keyword">CREATE TABLE</span> ${tableName} (\n`;
    const columns = document.querySelectorAll(".column-row");
    const columnDefinitions = [];
    const constraints = [];

    columns.forEach((column) => {
      const columnId = column.id;
      const columnName = document.getElementById(`${columnId}-name`).value || "column_name";
      const dataType = document.getElementById(`${columnId}-type`).value || "INT";

      let columnDef = `  ${columnName} <span class="sql-type">${dataType}</span>`;

      // Check constraints
      const isPK = document.getElementById(`${columnId}-pk`).checked;
      const isUnique = document.getElementById(`${columnId}-unique`).checked;
      const isNotNull = document.getElementById(`${columnId}-notnull`).checked;
      const isAutoIncrement = document.getElementById(`${columnId}-autoincrement`).checked;
      const hasDefault = document.getElementById(`${columnId}-default`).checked;
      const isForeignKey = document.getElementById(`${columnId}-fk`).checked;

      if (isNotNull) {
        columnDef += ` <span class="sql-constraint">NOT NULL</span>`;
      }
      if (isAutoIncrement) {
        columnDef += ` <span class="sql-constraint">AUTO_INCREMENT</span>`;
      }
      if (hasDefault) {
        const defaultValue = document.getElementById(`${columnId}-default-value`).value || "NULL";
        columnDef += ` <span class="sql-constraint">DEFAULT</span> ${defaultValue}`;
      }
      if (isPK) {
        constraints.push(`  <span class="sql-constraint">PRIMARY KEY</span> (${columnName})`);
      }
      if (isUnique) {
        constraints.push(`  <span class="sql-constraint">UNIQUE</span> (${columnName})`);
      }
      if (isForeignKey) {
        const refTable = document.getElementById(`${columnId}-ref-table`).value;
        const refColumn = document.getElementById(`${columnId}-ref-column`).value;
        if (refTable && refColumn) {
          constraints.push(`  <span class="sql-constraint">FOREIGN KEY</span> (${columnName}) <span class="sql-constraint">REFERENCES</span> ${refTable}(${refColumn})`);
        }
      }

      columnDefinitions.push(columnDef);
    });

    sql += columnDefinitions.join(",\n");

    if (constraints.length > 0) {
      sql += ",\n" + constraints.join(",\n");
    }

    sql += "\n);";

    sqlPreview.innerHTML = sql;
  }

  // Event listeners for generating SQL
  generateSqlBtn.addEventListener("click", generateSQL);
  tableForm.addEventListener("submit", function (e) {
    e.preventDefault();
    generateSQL();
    alert("Table creation SQL has been generated. In a real application, this would be executed against your database.");
  });

  // Add first column by default
  addColumn();
});

// Function to remove a column row
function removeColumn(id) {
  console.log(id);
  document.getElementById(id).remove();
}

// Function to add a new column
function addColumn() {
  console.log("Adding column");
  columnCounter++;
  const columnId = `column-${columnCounter}`;
  let currentColumnCount = document.querySelectorAll(".column-row").length;

  // Template literal for column HTML
  const columnHtml = `
    <div class="column-row" id="${columnId}">
      <button type="button" class="column-remove" onclick="removeColumn('${columnId}')">&times;</button>
      <h3>Column #${columnCounter - currentColumnCount}</h3>
      <div class="column-grid">
        <div class="form-group">
          <label for="${columnId}-name">Column Name</label>
          <input type="text" id="${columnId}-name" class="colNameInput" name="${columnId}-name" placeholder="Enter column name" required>
        </div>
        <div class="form-group">
          <label for="${columnId}-type">Data Type</label>
          <select id="${columnId}-type" class="colDataType" name="${columnId}-type" required>
            ${dataTypes.map(type => `<option value="${type}">${type}</option>`).join("")}
          </select>
        </div>
      </div>
      <div class="constraints-section">
        <h4 class="constraints-title">Constraints</h4>
        <div class="constraints-grid">
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-pk" name="${columnId}-constraints" value="PK">
            <label for="${columnId}-pk">Primary Key</label>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-unique" name="${columnId}-constraints" value="UK">
            <label for="${columnId}-unique">Unique</label>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-notnull" name="${columnId}-constraints" value="NN">
            <label for="${columnId}-notnull">Not Null</label>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-autoincrement" name="${columnId}-constraints" value="AUT">
            <label for="${columnId}-autoincrement">Auto Increment</label>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-default" name="${columnId}-constraints" value="DEF" onchange="toggleDefaultSection('${columnId}')">
            <label for="${columnId}-default">Default Value</label>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" id="${columnId}-fk" name="${columnId}-constraints" value="FK" onchange="toggleFKSection('${columnId}')">
            <label for="${columnId}-fk">Foreign Key</label>
          </div>
        </div>
        <div id="${columnId}-default-section" class="default-value-section">
          <div class="form-group">
            <label for="${columnId}-default-value">Default Value</label>
            <input type="text" id="${columnId}-default-value" name="${columnId}-default-value" placeholder="Enter default value">
          </div>
        </div>
        <div id="${columnId}-fk-section" class="foreign-key-section">
          <div class="fk-group">
            <div class="form-group">
              <label for="${columnId}-ref-table">Reference Table</label>
              <select id="${columnId}-ref-table" name="${columnId}-ref-table" onchange="updateDropColumn('${columnId}')">
              </select>
            </div>
            <div class="form-group">
              <label for="${columnId}-ref-column">Reference Column</label>
              <select id="${columnId}-ref-column" name="${columnId}-ref-column">
                <option value="">Select reference column</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  `; // End of columnHtml template literal

  // Create a temporary div, set its innerHTML to the column HTML, then append its first child to columnsArea
  const tempDiv = document.createElement("div");
  tempDiv.innerHTML = columnHtml;
  columnsArea.appendChild(tempDiv.firstElementChild);

  // Add event listener for the reference table change to populate reference columns dynamically
  document.getElementById(`${columnId}-ref-table`).addEventListener("change", function () {
    const selectedTable = this.value; // Get selected table
    const refColumnDropdown = document.getElementById(`${columnId}-ref-column`);
    // Clear previous options
    refColumnDropdown.innerHTML = '<option value="">Select reference column</option>';
    if (selectedTable && databases[selectedTable]) {
      // Get Primary Key columns from the selected table
      const primaryKeys = databases[selectedTable].filter((col) =>
        col.constraints.some(constraint => constraint.type === "PRIMARY_KEY")
      );
      // Populate Reference Column dropdown
      primaryKeys.forEach(pk => {
        const option = document.createElement("option");
        option.value = pk.name;
        option.textContent = pk.name;
        refColumnDropdown.appendChild(option);
      });
    }
  });

  // Initialize the select elements with proper styling
  const selects = document.querySelectorAll(`#${columnId} select`);
  selects.forEach(select => {
    select.style.width = "100%";
    select.style.padding = "10px";
    select.style.border = "1px solid #ccc";
    select.style.borderRadius = "4px";
  });
}

// Function to create a table using the dynamic column definitions
async function createTable() {
  console.log("Table creation called");
  let tableName = document.getElementById("tableName").value.trim();

  if (!tableName) {
    showNotification("Table name shouldn't be empty!", "error");
    return;
  } else if (/\s/.test(tableName)) {
    showNotification("Empty space is not allowed!", "error");
    return;
  } else if (!currentDatabase) {
    showNotification("No database selected!", "alert");
    return;
  } else if (databases[currentDatabase].includes(tableName)) {
    showNotification("Table name already exists in this database!", "error");
    return;
  }

  var columnContainer = document.querySelectorAll(".colNameInput");
  if (columnContainer.length < 1) {
    showNotification("Table must contain at least one column!", "error");
    return;
  }

  let duplicateCheck = [];
  for (const el of columnContainer) {
    if (el.value == "") {
      showNotification("Column name shouldn't be empty!", "error");
      return;
    }
    if (duplicateCheck.includes(el.value)) {
      showNotification("Same column names aren't allowed!", "error");
      return;
    }
    duplicateCheck.push(el.value);
  }
  
  // Check again if table already exists
  if (databases[currentDatabase].includes(tableName)) {
    showNotification("Table name already exists in this database!", "error");
    return;
  }

  // Validate datatype selection for each column
  let columnDataTypes = document.querySelectorAll(".colDataType");
  for (const type of columnDataTypes) {
    if (type.value == "") {
      type.style.border = "1px solid red";
      showNotification("Select datatype", "error");
      return;
    }
  }

  // Get all column rows (assumes they are inside an element with class "content")
  let columns = document.querySelector(".content").querySelectorAll(".column-row");
  let columnList = [];

  let createObject = {
    action: "createTable",
    dbName: currentDatabase,
    tableName: tableName,
  };

  // For each dynamic column box, extract its values
  for (const element of columns) {
    let colName = element.getElementsByTagName("input")[0].value;
    let type = element.getElementsByTagName("select")[0].value;
    let constraintList = [];

    Array.from(element.querySelector(".constraints-grid").getElementsByTagName("div")).forEach(el => {
      if (el.getElementsByTagName("input")[0].checked) {
        if (el.getElementsByTagName("input")[0].value == "DEF") {
          let cons = {
            constraint: el.getElementsByTagName("input")[0].value,
            DefValue: el.parentElement.nextElementSibling.getElementsByTagName("input")[0].value,
          };
          constraintList.push(cons);
        } else if (el.getElementsByTagName("input")[0].value == "FK") {
          let cons = {
            constraint: el.getElementsByTagName("input")[0].value,
            refTable: el.parentElement.nextElementSibling.nextElementSibling.getElementsByTagName("select")[0].value,
            refColumn: el.parentElement.nextElementSibling.nextElementSibling.getElementsByTagName("select")[1].value,
          };
          constraintList.push(cons);
        } else {
          let cons = {
            constraint: el.getElementsByTagName("input")[0].value,
          };
          constraintList.push(cons);
        }
      }
    });

    let obj = {
      colName: colName,
      datatype: type,
      consList: constraintList,
    };
    columnList.push(obj);
  }

  createObject.columnList = columnList;
  console.log(createObject);

  // Clear the columns area after table creation
  document.getElementById("columnsArea").innerHTML = "";
  createFetch(createObject); // This should call your backend to create the table
  showNotification("Table created successfully");
}

console.log(currentDatabase);

