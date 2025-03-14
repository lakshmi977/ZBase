window.onload = async function () {
  if (window.location.pathname.endsWith("insert.html")) {
    window.location.href = "dashboard.html"; // Redirect if already logged in
  }
};

const txtFormat = ["STRING", "CHAR"];

console.log("Page Loaded...");

// Function to generate input fields dynamically
function generateInputFields(recordCount) {
  return columnData
    .map((col) => {
      let inputField = "";

      if (col.dataType === "BOOL") {
        inputField = `
            <select name="${col.name.toLowerCase()}[]" required>
              <option value="" disabled selected>Select</option>
              ${col.options
                .map((option) => `<option value="${option}">${option}</option>`)
                .join("")}
            </select>
          `;
      } else if (col.dataType === "BLOB") {
        inputField = `<input type="file" name="${col.name.toLowerCase()}[]" class="file-input">`;
      } else if (["CHAR", "STRING"].includes(col.dataType)) {
        inputField = `<input type="text" name="${col.name.toLowerCase()}[]" required>`;
      } else if (col.dataType === "INT") {
        inputField = `<input type="text" name="${col.name.toLowerCase()}[]" required oninput="this.value = this.value.replace(/\D/g, '')">`;
      } else {
        inputField = `<input type="number" name="${col.name.toLowerCase()}[]" required>`;
      }

      return `
          <div class="input-row">
            <div class="column-name">${col.name}</div>
            <div class="input-field">${inputField}</div>
          </div>
        `;
    })
    .join("");
}

// Function to add a new record
function addRow() {
  const container = document.getElementById("records-container");
  const recordCount =
    container.getElementsByClassName("record-container").length + 1;

  const newRecord = document.createElement("div");
  newRecord.className = "record-container";
  newRecord.innerHTML = `
        <div class="record-header">
            <span class="record-title">Record #${recordCount}</span>
            <button class="remove-button">Remove</button>
        </div>
        <div class="record-body">
            ${generateInputFields(recordCount)}
        </div>
    `;

  container.appendChild(newRecord);
}

// Remove record when remove button is clicked
document.addEventListener("click", function (e) {
  if (e.target && e.target.classList.contains("remove-button")) {
    e.target.closest(".record-container").remove();

    // Update record numbers
    const containers = document.getElementsByClassName("record-container");
    for (let i = 0; i < containers.length; i++) {
      containers[i].querySelector(".record-title").textContent = `Record #${
        i + 1
      }`;
    }
  }
});

// File input label update
document.addEventListener("change", function (e) {
  if (e.target && e.target.classList.contains("file-input")) {
    const fileName =
      e.target.files.length > 0 ? e.target.files[0].name : "Choose file...";
    e.target.nextElementSibling.textContent = fileName;
  }
});

// Function to convert file to Base64
function convertToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = function () {
      const base64String = reader.result.split(",")[1];
      resolve(base64String);
    };

    reader.onerror = function () {
      reject(new Error("Error reading file"));
    };

    reader.readAsDataURL(file); // Read file as Data URL
  });
}

// Insert function
async function insert() {
  console.log("Insert function triggered");

  const insertObject = {
    dbName: currentDatabase,
    tableName: currentTable,
    values: {},
  };

  const promises = [];

  document.querySelectorAll(".input-row").forEach((el) => {
    const colName = el.querySelector(".column-name").textContent.trim();
    const input = el.getElementsByTagName("input")[0];

    if (input && input.type === "file" && input.files.length > 0) {
      const filePromise = convertToBase64(input.files[0]).then((base64String) => {
        insertObject.values[colName] = base64String;
      });
      promises.push(filePromise);
    } else {
      insertObject.values[colName] = input.value;
    }
  });

  await Promise.all(promises);
  insertFetch(insertObject);
}

// Prevent multiple form submissions
document.getElementById("insertForm").addEventListener("submit", function (e) {
  e.preventDefault();

  if (!this.dataset.submitted) {
    this.dataset.submitted = "true";
    insert();

    // Reset the flag after 1 second to allow future submissions
    setTimeout(() => {
      delete this.dataset.submitted;
    }, 1000);
  }
});
