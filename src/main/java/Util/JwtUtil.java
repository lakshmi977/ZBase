//window.onload = async function () {
//  if (window.location.pathname.endsWith("create.html")) {
//    window.location.href = "dashboard.html"; // Redirect if already logged in
//  }
//};
//
//document.getElementById("selectedDb").textContent += currentDatabase;
//
//
//columnsArea = document.getElementById("columnsArea");
//console.log(columnsArea);
//console.log(databases);
//
////   const addColumnBtn = document.getElementById("addColumnBtn");
//generateSqlBtn = document.getElementById("generateSqlBtn");
//sqlPreview = document.getElementById("sqlPreview");
//tableForm = document.getElementById("tableForm");
//
//// Sample data types for dropdown
//dataTypes = ["INT", "STRING", "BOOLEAN", "FLOAT", "BLOB"];
//
//// Sample tables for foreign key references
//sampleTables = ["users", "products", "categories", "orders", "customers"];
//
//document.addEventListener("DOMContentLoaded", function () {
//  console.log("kdomo");
//
//  // Sample columns for foreign key references
//  sampleColumns = {
//    users: ["id", "username", "email"],
//    products: ["id", "sku", "product_code"],
//    categories: ["id", "category_code"],
//    orders: ["id", "order_number"],
//    customers: ["id", "customer_code"],
//  };
//
//  // Remove column function
//  window.removeCons = function (columnId) {
//    const columnElement = document.getElementById(columnId);
//    if (columnElement) {
//      columnElement.remove();
//    }
//  };
//
//  // Toggle default value section
//  window.toggleDefaultSection = function (columnId) {
//    const checkbox = document.getElementById(`${columnId}-default`);
//    const section = document.getElementById(`${columnId}-default-section`);
//
//    if (checkbox.checked) {
//      section.style.display = "block";
//    } else {
//      section.style.display = "none";
//    }
//  };
//
//  // Toggle foreign key section
//  window.toggleFKSection = function (columnId) {
//    const checkbox = document.getElementById(`${columnId}-fk`);
//    const section = document.getElementById(`${columnId}-fk-section`);
//
//    if (checkbox.checked) {
//      section.style.display = "block";
//    } else {
//      section.style.display = "none";
//    }
//  };
//
//  // Update reference columns based on selected table
//  window.updateDropColumn = function (columnId) {
//    const refTable = document.getElementById(`${columnId}-ref-table`).value;
//    const refColumnSelect = document.getElementById(`${columnId}-ref-column`);
//
//    // Clear previous options
//    refColumnSelect.innerHTML =
//      '<option value="">Select reference column</option>';
//
//    // Add new options based on selected table
//    if (refTable && sampleColumns[refTable]) {
//      sampleColumns[refTable].forEach((column) => {
//        const option = document.createElement("option");
//        option.value = column;
//        option.textContent = column;
//        refColumnSelect.appendChild(option);
//      });
//    }
//  };
//
//  // Generate SQL statement
//  function generateSQL() {
//    const tableName =
//      document.getElementById("tableName").value || "table_name";
//    let sql = `<span class="sql-keyword">CREATE TABLE</span> ${tableName} (\n`;
//
//    const columns = document.querySelectorAll(".column-row");
//    const columnDefinitions = [];
//    const constraints = [];
//
//    columns.forEach((column) => {
//      const columnId = column.id;
//      const columnName =
//        document.getElementById(`${columnId}-name`).value || "column_name";
//      const dataType =
//        document.getElementById(`${columnId}-type`).value || "INT";
//
//      let columnDef = `  ${columnName} <span class="sql-type">${dataType}</span>`;
//
//      // Check constraints
//      const isPK = document.getElementById(`${columnId}-pk`).checked;
//      const isUnique = document.getElementById(`${columnId}-unique`).checked;
//      const isNotNull = document.getElementById(`${columnId}-notnull`).checked;
//      const isAutoIncrement = document.getElementById(
//        `${columnId}-autoincrement`
//      ).checked;
//      const hasDefault = document.getElementById(`${columnId}-default`).checked;
//      const isForeignKey = document.getElementById(`${columnId}-fk`).checked;
//
//      if (isNotNull) {
//        columnDef += ` <span class="sql-constraint">NOT NULL</span>`;
//      }
//
//      if (isAutoIncrement) {
//        columnDef += ` <span class="sql-constraint">AUTO_INCREMENT</span>`;
//      }
//
//      if (hasDefault) {
//        const defaultValue =
//          document.getElementById(`${columnId}-default-value`).value || "NULL";
//        columnDef += ` <span class="sql-constraint">DEFAULT</span> ${defaultValue}`;
//      }
//
//      if (isPK) {
//        constraints.push(
//          `  <span class="sql-constraint">PRIMARY KEY</span> (${columnName})`
//        );
//      }
//
//      if (isUnique) {
//        constraints.push(
//          `  <span class="sql-constraint">UNIQUE</span> (${columnName})`
//        );
//      }
//
//      if (isForeignKey) {
//        const refTable = document.getElementById(`${columnId}-ref-table`).value;
//        const refColumn = document.getElementById(
//          `${columnId}-ref-column`
//        ).value;
//
//        if (refTable && refColumn) {
//          constraints.push(
//            `  <span class="sql-constraint">FOREIGN KEY</span> (${columnName}) <span class="sql-constraint">REFERENCES</span> ${refTable}(${refColumn})`
//          );
//        }
//      }
//
//      columnDefinitions.push(columnDef);
//    });
//
//    sql += columnDefinitions.join(",\n");
//
//    if (constraints.length > 0) {
//      sql += ",\n" + constraints.join(",\n");
//    }
//
//    sql += "\n);";
//
//    sqlPreview.innerHTML = sql;
//  }
//
//  // Event listeners
//  generateSqlBtn.addEventListener("click", generateSQL);
//
//  tableForm.addEventListener("submit", function (e) {
//    e.preventDefault();
//    generateSQL();
//    alert(
//      "Table creation SQL has been generated. In a real application, this would be executed against your database."
//    );
//  });
//
//  // Add first column by default
//  addColumn();
//});
//
//function removeColumn(id) {
//  console.log(id);
//  document.getElementById(id).remove();
//}
//
//function addColumn() {
//  console.log("varthe");
//  
//  columnCounter++;
//  const columnId = `column-${columnCounter}`;
//  let currentColumnCount = document.querySelectorAll(".column-row").length;
//
//  const columnHtml = `
//                <div class="column-row" id="${columnId}">
//                    <button type="button" class="column-remove" onclick="removeColumn('${columnId}')">&times;</button>
//                    <h3>Column #${columnCounter - currentColumnCount}</h3>
//  
//                    <div class="column-grid">
//                        <div class="form-group">
//                            <label for="${columnId}-name">Column Name</label>
//                            <input type="text" id="${columnId}-name" class="colNameInput" name="${columnId}-name" placeholder="Enter column name" required>
//                        </div>
//  
//                        <div class="form-group">
//                            <label for="${columnId}-type">Data Type</label>
//                            <select id="${columnId}-type" class="colDataType" name="${columnId}-type" required>
//                                ${dataTypes
//                                  .map(
//                                    (type) =>
//                                      `<option value="${type}">${type}</option>`
//                                  )
//                                  .join("")}
//                            </select>
//                        </div>
//                    </div>
//  
//                    <div class="constraints-section">
//                        <h4 class="constraints-title">Constraints</h4>
//                        <div class="constraints-grid">
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-pk" name="${columnId}-constraints" value="PK">
//                                <label for="${columnId}-pk">Primary Key</label>
//                            </div>
//  
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-unique" name="${columnId}-constraints" value="UK">
//                                <label for="${columnId}-unique">Unique</label>
//                            </div>
//  
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-notnull" name="${columnId}-constraints" value="NN">
//                                <label for="${columnId}-notnull">Not Null</label>
//                            </div>
//  
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-autoincrement" name="${columnId}-constraints" value="AUT">
//                                <label for="${columnId}-autoincrement">Auto Increment</label>
//                            </div>
//  
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-default" name="${columnId}-constraints" value="DEF" onchange="toggleDefaultSection('${columnId}')">
//                                <label for="${columnId}-default">Default Value</label>
//                            </div>
//  
//                            <div class="checkbox-group">
//                                <input type="checkbox" id="${columnId}-fk" name="${columnId}-constraints" value="FK" onchange="toggleFKSection('${columnId}')">
//                                <label for="${columnId}-fk">Foreign Key</label>
//                            </div>
//                        </div>
//  
//                        <div id="${columnId}-default-section" class="default-value-section">
//                            <div class="form-group">
//                                <label for="${columnId}-default-value">Default Value</label>
//                                <input type="text" id="${columnId}-default-value" name="${columnId}-default-value" placeholder="Enter default value">
//                            </div>
//                        </div>
//  
//                        <div id="${columnId}-fk-section" class="foreign-key-section">
//                            <div class="fk-group">
//                                <div class="form-group">
//                                    <label for="${columnId}-ref-table">Reference Table</label>
//                                    <select id="${columnId}-ref-table" name="${columnId}-ref-table" onchange="updateDropColumn('${columnId}')">
//                                    </select>
//                                </div>
//  
//                                <div class="form-group">
//                                    <label for="${columnId}-ref-column">Reference Column</label>
//                                    <select id="${columnId}-ref-column" name="${columnId}-ref-column">
//                                        <option value="">Select reference column</option>
//                                    </select>
//                                </div>
//                            </div>
//                        </div>
//                    </div>
//                </div>
//  `; // <-- Fixed: closing backtick and semicolon
//
//  columnsArea.insertAdjacentHTML("beforeend", columnHtml);
//};