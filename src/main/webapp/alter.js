window.onload = async function() {
	if (window.location.pathname.endsWith("alter.html")) {
		window.location.href = "dashboard.html"; // Redirect if already logged in
	}
};

currentTab="Add Column";

loadTableData("referenceTable");
document.getElementById("tabs").addEventListener("click", function(event) {
	const tab = event.target.closest(".tab"); // Ensure clicked element is a tab
	if (!tab) return;
	document.querySelectorAll(".tab").forEach((t) => {
		t.classList.remove("active");
	});

	tab.classList.add("active");
	currentTab = tab.innerText;

	document.querySelectorAll(".tab-content").forEach((content) => {
		content.classList.remove("active");
	});

	const tabId = tab.getAttribute("data-tab");
	document.getElementById(tabId).classList.add("active");
	if (currentTab == "Add Column") {
		loadTableData("referenceTable");
	} else if (currentTab == "Drop Column") {
		loadColumnData("columnToDrop");
	} else if (currentTab == "Rename Column") {
		loadColumnData("columnToRename");
	} else if (currentTab == "Change Datatype") {
		loadColumnData("columnToAlter");
	} else if (currentTab == "Add Constraint") {
		loadColumnData("constraintColumn");
		loadTableData("referenceTable");
	} else if (currentTab == "Drop Constraint") {
		loadColumnData("dropConstraintColumn");
		document
			.getElementById("dropConstraintColumn")
			.addEventListener("change", () => {
				loadConstraintData("constraintToDrop");
			});
	}
});

// Toggle additional fields for constraints in Add Column tab
document.getElementById("addDefault").addEventListener("change", function() {
	toggleVisibility("defaultValueField", this.checked);
});

document
	.getElementById("addForeignKey")
	.addEventListener("change", function() {
		toggleVisibility("foreignKeyField", this.checked);
	});

// Toggle additional fields for constraints in Add Constraint tab

// Show/hide constraint fields based on selection in Add Constraint
constraintTypes = document.querySelectorAll('input[name="constraintType"]');
constraintTypes.forEach((type) => {
	type.addEventListener("change", () => {
		// Hide all constraint fields
		document.querySelectorAll(".constraint-fields").forEach((field) => {
			field.style.display = "none";
		});

		// Show the selected constraint fields
		const selectedType = type.value;
		if (selectedType === "default") {
			console.log("hello");
			document.getElementById("defaultValueBox").style.display = "block";
		} else if (selectedType === "foreignKey") {
			document.getElementById("foreignKeyFields").style.display = "block";
		}
	});
});

function toggleVisibility(elementId, isVisible) {
	const element = document.getElementById(elementId);
	if (isVisible) {
		element.classList.remove("hidden");
	} else {
		element.classList.add("hidden");
	}
}

// Populate constraint dropdown based on column selection in Drop Constraint
dropConstraintColumn = document.getElementById("dropConstraintColumn");
if (dropConstraintColumn) {
	dropConstraintColumn.addEventListener("change", () => {
		const constraintToDrop = document.getElementById("constraintToDrop");

		// Clear existing options
		while (constraintToDrop.options.length > 1) {
			constraintToDrop.remove(1);
		}
	});
}

function loadColumnData(id) {
	let selectElement = document.getElementById(id);
	selectElement.innerHTML = '<option value="">--Select column--</option>'; // Reset previous options

	columnData.forEach((column) => {
		let option = document.createElement("option");
		option.value = column.name;
		option.textContent = column.name;
		selectElement.appendChild(option);
	});
}

function loadConstraintData(id) {
	columnData.forEach((column) => {
		console.log(column.name);
		console.log(document.getElementById("dropConstraintColumn").value);
		if (column.name == document.getElementById("dropConstraintColumn").value) {
			column.constraints.forEach((cons) => {
				let option = document.createElement("option");
				option.value = cons.type;
				option.textContent = cons.type;
				document.getElementById(id).appendChild(option);
			});
		}
	});
	console.log("hello");
	console.log(document.getElementById(id).options.length < 2);
	if (document.getElementById(id).options.length < 2) {
		console.log(document.getElementById(id));
		document.getElementById(id).options[0].text = "No constraint available";
	}
}

function loadTableData(id) {
	let selectElement = document.getElementById(id);

	// Clear previous options (optional, to avoid duplicate entries)

	selectElement.innerHTML = "";

	// Iterate over the object keys (table names)
	Object.keys(allTable).forEach((tableName) => {
		let option = document.createElement("option");
		option.value = tableName;
		option.textContent = tableName;
		selectElement.appendChild(option);
	});

	// If no tables are available
	if (selectElement.options.length === 0) {
		let noDataOption = document.createElement("option");
		noDataOption.textContent = "No tables available";
		selectElement.appendChild(noDataOption);
	}
}

function loadPrimaryKeyData(id) {
	let referenceTable = document.getElementById("referenceTable").value;
	let selectElement = document.getElementById(id);

	// Clear previous options
	selectElement.innerHTML = "";

	if (allTable.hasOwnProperty(referenceTable)) {
		let primaryKey = allTable[referenceTable]; // ["primaryColumn", "datatype"]

		let option = document.createElement("option");
		option.value = primaryKey[0]; // Primary column name
		option.textContent = `${primaryKey[0]} (${primaryKey[1]})`; // Display column + type
		selectElement.appendChild(option);
	} else {
		let noDataOption = document.createElement("option");
		noDataOption.textContent = "No primary key available";
		selectElement.appendChild(noDataOption);
	}
}

// Event Listener to update primary key when a table is selected
document
	.getElementById("referenceTable")
	.addEventListener("change", function() {
		loadPrimaryKeyData("referenceColumn");
	});

function execute() {
	alterJson = {
		dbName: currentDatabase,
		tableName: currentTable,
	}; // Reset JSON object
	switch (currentTab) {
		case "Add Column":
			alterJson.action = "ADD_COLUMN";
			alterJson.columnName = document.getElementById("newColumnName").value;
			alterJson.datatype = document.getElementById("dataType").value;

			// Collect constraints
			let conslist = [];
			if (document.getElementById("addPrimaryKey").checked)
				conslist.push({ constraint: "PK" });
			if (document.getElementById("addUniqueKey").checked)
				conslist.push({ constraint: "UK" });
			if (document.getElementById("addNotNull").checked)
				conslist.push({ constraint: "NN" });
			if (document.getElementById("addAutoIncrement").checked)
				conslist.push({ constraint: "AUT" });

			if (document.getElementById("addDefault").checked) {
				conslist.push({
					constraint: "DEF",
					default: document.getElementById("defaultValue").value,
				});
			}

			if (document.getElementById("addForeignKey").checked) {
				conslist.push({
					constraint: "FK",
					fkTable: document.getElementById("referenceTable").value,
					fkColumn: document.getElementById("referenceColumn").value,
				});
			}

			alterJson.conslist = conslist;
			break;

		case "Drop Column":
			alterJson.action = "DROP_COLUMN";
			alterJson.columnName = document.getElementById("columnToDrop").value;
			break;

		case "Rename Column":
			alterJson.action = "RENAME_COLUMN";
			alterJson.oldColumnName = document.getElementById("columnToRename").value;
			alterJson.newColumnName = document.getElementById("newName").value;
			break;

		case "Change Datatype":
			alterJson.action = "CHANGE_DATATYPE";
			alterJson.columnName = document.getElementById("columnToAlter").value;
			alterJson.datatype = document.getElementById("newDataType").value;
			break;

		case "Add Constraint":
			alterJson.action = "ADD_CONSTRAINT";
			alterJson.columnName = document.getElementById("constraintColumn").value;

			let cons = {};

			let selectedConstraint = document.querySelector(
				'input[name="constraintType"]:checked'
			);
			if (selectedConstraint) {
				cons.constraint = selectedConstraint.value;

				if (selectedConstraint.value === "default") {
					cons.default = document.getElementById("defaultValue").value;
				}

				if (selectedConstraint.value === "foreignKey") {
					cons.fkTable = document.getElementById("referenceTable").value;
					cons.fkColumn = document.getElementById("referenceColumn").value;
				}
			} else {
				alter("Choose a constraint");
				return;
			}
			alterJson.constraints = cons;
			break;

		case "Drop Constraint":
			alterJson.action = "DROP_CONSTRAINT";
			alterJson.columnName = document.getElementById(
				"dropConstraintColumn"
			).value;
			alterJson.constraintName =
				document.getElementById("constraintToDrop").value;
			break;
	}

	console.log("alter json herer",alterJson);
	alterServlet(alterJson); // For debugging
	// You can now send this JSON to your backend for processing
}
