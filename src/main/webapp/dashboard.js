async function logout() {
	try {
		const response = await fetch(
			"http://localhost:8080/Database/LogoutServlet",
			{
				method: "POST",
				credentials: "include", // ✅ Ensures cookies are sent
			}
		);

		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}

		const result = await response.json(); // ✅ Parse JSON response

		// 🔹 Redirect to login page after logout
		window.location.href = "LandingPage.html";
	} catch (error) {
		console.error("Logout failed:", error);
	}
}

window.onload = async function () {
	try {
		const response = await fetch(
			"http://localhost:8080/Database/CheckAuthServlet",
			{
				method: "GET",
				credentials: "include", // 🔥 Sends cookies for authentication
			}
		);

		if (!response.ok) {
			throw new Error(`HTTP Error: ${response.status}`);
		}

		const data = await response.json();
		console.log("Auth Check Response:", data);

		if (
			!data.authenticated &&
			window.location.pathname.endsWith("dashboard.html")
		) {
			console.log("User not authenticated! Redirecting...");
			window.location.href = "LandingPage.html"; // Redirect only if not authenticated
		}
	} catch (error) {
		console.error("Auth check failed:", error);
		window.location.href = "LandingPage.html"; // Redirect in case of error
	}
	try {
		console.log("mister");
		const response = await fetch(
			"http://localhost:8080/Database/Service/FetchDataServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		console.log("mister2");
		const result = await response.json();
		databases = result.DATABASES;
		console.log(databases);
	} catch (error) {
		console.error("Error:", error);
	}

	let operation = document.getElementById("operation");
	operation.value = "select";

	let dbDeleteBox = document.querySelector(".dbDeleteContainer");
	dbDeleteBox.style.display = "none";
	document.querySelector(".tableDeleteContainer").style.display = "none";
	updateExplorer();

	if (databases != null && Object.keys(databases).length > 0) {
		updateExplorer();
	}
	console.log(Object.keys(databases).length);
};

let selectAllValues;
let databases = {};
let allTable = {};
let currentDatabase = "";
let currentTable = null;
let columnData = [];

const constraints = [
	{ value: "NONE", text: "None" },
	{ value: "NN", text: "not Null" },
	{ value: "UK", text: "Unique key" },
	{ value: "DEF", text: "default" },
	{ value: "PK", text: "Primary key" },
	{ value: "FK", text: "Foreign key" },
	{ value: "AI", text: "Auto Increment" },
];

const datatypes = [
	{ value: "STRING", text: "String" },
	{ value: "CHAR", text: "char" },
	{ value: "INT", text: "Integer" },
	{ value: "FLOAT", text: "Float" },
	{ value: "BOOLEAN", text: "Boolean" },
	{ value: "BLOB", text: "Blob" },
];

function showNotification(message, type) {
	const notificationBox = document.getElementById("notificationBox");
	notificationBox.innerText = message;
	notificationBox.className = `notification ${type}`;
	notificationBox.style.display = "block";

	if (type === "error") {
		notificationBox.classList.add("shake"); // Add shake effect
		setTimeout(() => {
			notificationBox.classList.remove("shake"); // Remove shake after animation
		}, 400); // Match the animation duration
	}

	setTimeout(() => {
		notificationBox.style.display = "none";
	}, 3000);
}


// showNotification("Database created successfully!", "success");
// showNotification("Failed to create database!", "error");

function showCreateDatabaseUI() {
	const overlay = document.getElementById("modalOverlay");
	overlay.style.display = "flex";
	setTimeout(() => {
		overlay.querySelector(".modal").style.transform = "translateY(0)";
		overlay.querySelector(".modal").style.opacity = "1";
	}, 10);
	document.getElementById("dbName").value = "";
	document.getElementById("dbMessage").textContent = "";
	document.getElementById("dbMessage").style.opacity = "0";
}

function hideCreateDatabaseUI() {
	const overlay = document.getElementById("modalOverlay");
	const modal = overlay.querySelector(".modal");
	modal.style.transform = "translateY(20px)";
	modal.style.opacity = "0";
	setTimeout(() => {
		overlay.style.display = "none";
	}, 300);
}

function showCreateTableUI() {
	document.querySelector(".tableDisplay").style.display = "none";
	console.log(document.querySelector(".tableDisplay"));

	// document.querySelector(".query-box").style.display = "none";
	// document.querySelector(".tableDeleteContainer").style.display = "none";
	// document.querySelector(".dbDeleteContainer").style.display = "flex";

	if (currentDatabase) return;
	// const section = document.getElementById("tableSection");
	// section.style.display = "block";
	// document.getElementById("selectedDb").textContent = currentDatabase;

	// // Clear inputs in the table creation input box
	// document.getElementById("tableName").value = "";
	// document.getElementById("columnInputs").innerHTML = "";
	// document.getElementById("tableMessage").textContent = "";
	// document.getElementById("tableMessage").classList.remove("show");

	let create = document.createElement("div");
	dispatchEvent.id = "createTableInDb";

	operator(create);
}

function updateExplorer() {
	const explorer = document.getElementById("dbExplorer");
	const expandedDb = document.querySelector(".database-item.expanded"); // Store expanded DB

	explorer.innerHTML = "";
	console.log(databases);

	Object.keys(databases).forEach((dbName) => {
		const dbDiv = document.createElement("div");
		dbDiv.classList.add("database-item");

		const headerDiv = document.createElement("div");
		headerDiv.classList.add("header");

		const hasTables = databases[dbName] && databases[dbName].length > 0;
		const arrow = document.createElement("i");

		arrow.classList.add("fas", hasTables ? "fa-chevron-right" : "fa-database");
		arrow.style.transition = "transform 0.3s ease";
		arrow.style.width = "20px";

		const nameSpan = document.createElement("span");
		nameSpan.textContent = dbName;


		let removeDBBtn = document.createElement("i");
		removeDBBtn.classList.add("fa-solid", "fa-xmark");
		removeDBBtn.classList.add("removeDBBtn");
		removeDBBtn.onclick = function() {
			dropDb()
		}

		let nameArrow = document.createElement("div");
		nameArrow.classList.add("nameArrowHolder");
		nameArrow.appendChild(arrow);
		nameArrow.appendChild(nameSpan);
		headerDiv.appendChild(nameArrow);
		headerDiv.appendChild(removeDBBtn);



		nameArrow.onclick = async function() {
			currentDatabase = headerDiv.getElementsByTagName("i")[0].innerText;
			const isExpanded = dbDiv.classList.contains("expanded");
			document.querySelector(".tableDisplay").style.display = "none";

			// document.getElementById("selectedDb").textContent+= currentDatabase;

			document.querySelectorAll(".database-item").forEach((item) => {
				item.classList.remove("expanded");
				currentDatabase = null;
				currentTable = null;
				selectDatabase(null, null);
				document.getElementById("newTable").style.display = "flex";
				selectTable(currentTable);

				const tableList = item.querySelector(".table-list");
				if (tableList) {
					console.log("Hello ram");

					tableList.style.maxHeight = "0px";
					tableList.style.opacity = "0";
				}
				item.querySelector("i").style.transform = "rotate(0deg)";
			});

			if (!isExpanded) {
				// console.log("Ithukkulla podu");

				selectDatabase(dbName, dbDiv.parentElement);
				// operation(document.querySelector(".active"));

				document.getElementById("newTable").style.display = "flex";

				if (hasTables) {
					const tableList = dbDiv.querySelector(".table-list");
					if (tableList) {
						tableList.style.maxHeight = "fit-content";
						tableList.style.opacity = "1";
						arrow.style.transform = "rotate(90deg)";
					}
				}
				dbDiv.classList.add("expanded");
				await tablePrimaryFetch();
			} else {
				document.getElementById("newTable").style.display = "none";
				// document.getElementById("tableSection").style.display = "none";
			}
			console.log(isExpanded);
		};

		dbDiv.appendChild(headerDiv);

		if (hasTables) {
			const tableList = document.createElement("div");
			tableList.classList.add("table-list");

			if (expandedDb && expandedDb.textContent === dbName) {
				tableList.style.maxHeight = "fit-content";
				tableList.style.opacity = "1";
				arrow.style.transform = "rotate(90deg)";
				dbDiv.classList.add("expanded");
			} else {
				document.getElementById("newTable").style.display = "none";
				tableList.style.maxHeight = "0px";
				tableList.style.opacity = "0";
			}

			databases[dbName].forEach((table) => {
				const tableDiv = document.createElement("div");
				tableDiv.classList.add("table-item");

				const tableIcon = document.createElement("i");
				tableIcon.classList.add("fas", "fa-table");

				const tableNameSpan = document.createElement("span");
				tableNameSpan.textContent = table; // FIXED: Use table directly

				tableDiv.appendChild(tableIcon);
				tableDiv.appendChild(tableNameSpan);

				//table onclick event
				tableDiv.onclick = async function() {
					currentTable = tableDiv.getElementsByTagName("i")[0].innerText;
					selectTable(this);
					document.querySelector(".tableDisplay").style.display = "flex";

					const requestData = {
						dbName: currentDatabase,
						tableName: currentTable,
					};

					//Removing the operatorSpace's first element

					document.getElementById("operatorSpace").firstChild.remove();
					document.querySelector(".navbar").style.display = "flex";

					await fetchTableServlet();
					// document.querySelector(".query-box").style.display = "flex";
					document.querySelector(".tableDeleteContainer").style.display =
						"flex";
					// document.getElementById("tableSection").style.display = "none";
					document.querySelector(".dbDeleteContainer").style.display = "none";

					document.querySelector(".navbar").style.disabled = "flex";


					console.log("running")
					console.log(columnData)
					let insert = document.getElementById("operationInsert");

					document.querySelectorAll(".active").forEach((el) => {
						el.classList.remove("active");
					})

					insert.classList.add("active");
					operation(insert);

				};

				tableList.appendChild(tableDiv);
			});

			dbDiv.appendChild(tableList);
		}

		explorer.appendChild(dbDiv);
	});
}
async function createDatabase() {
	const dbName = document.getElementById("dbName").value.trim();
	if (!dbName) {
		alert("Database name cannot be empty!");
		return;
	}

	if (dbName in databases) {
		alert("Database already exists!");
		return;
	}
	let db = {
		dbName: dbName,
		action: "createDatabase",
	};

	console.log("dbname:", dbName)

	await createDatabaseFetch(db);

	document.getElementById("dbName").value = "";
	//  updateExplorer();

	const dbElements = document.querySelectorAll(".database-item");
	dbElements.forEach((el) => {
		if (el.querySelector("span").textContent === currentDatabase) {
			el.classList.add("selected");
		}
	});
	// document.getElementById("noDatabaseMessage").style.display = "none";

	setTimeout(() => {
		hideCreateDatabaseUI();
		const messageEl = document.getElementById("dbMessage");
		if (messageEl) {
			messageEl.classList.remove("show");
		}
	}, 150);
}

function selectDatabase(dbName, dbElement) {

	console.log("Table selection called");

	if (dbName == null) {
		document
			.querySelectorAll(".database-item")
			.forEach((el) => el.classList.remove("selected"));
		// document.getElementById("tableSection").style.display = "none";
		// document.querySelector(".dbDeleteContainer").style.display = "none";

		// document.querySelector(".query-box").style.display = "none";
		// document.querySelector(".tableDeleteContainer").style.display = "none";

		let noJQElement = document.createElement("div");
		noJQElement.id = "noElement";
		operation(noJQElement);

		//Hiding navbar
		document.querySelector(".tableDisplay").style.display = "none";

		//Hiding navbar
		document.querySelector(".navbar").style.display = "none";

		console.log("Hello ram");

		return;
	}

	currentDatabase = dbName;
	currentTable = null; //Changing current table to null when a new database is selected
	console.log(currentDatabase);

	let dummy = document.createElement("div");
	dummy.id = "createTableInDb";
	operation(dummy);

	// document.getElementById("selectedDb").textContent = dbName;

	//
	document
		.querySelectorAll(".database-item")
		.forEach((el) => el.classList.remove("selected"));
	document
		.querySelectorAll(".table-item")
		.forEach((el) => el.classList.remove("selected"));

	dbElement.classList.add("selected");
	// showCreateTableUI();
}

function cancelTablecreation() {
	// console.log(document.getElementById("c").childNodes);

	const overlay = document.getElementById("tableSection");
	overlay.style.display = "none";
	let dbDeleteBox = document.querySelector(".dbDeleteContainer");
	dbDeleteBox.style.display = "none";
}

function removeColumnInput(element) {
	element.remove();
}

function addCondition() {
	let condition_container = document.getElementById("condition_container");

	let addConditionBox = document.createElement("div");
	addConditionBox.classList.add("addConditionBox");

	let andOr = document.createElement("select");
	let options = [
		{ value: "AND", text: "AND" },
		{ value: "OR", text: "OR" },
	];

	options.forEach((optionData) => {
		let option = document.createElement("option");
		option.value = optionData.value;
		option.text = optionData.text;
		andOr.appendChild(option);
	});

	addConditionBox.appendChild(andOr);

	let colName = document.createElement("select");
	options = [{ value: "CN", text: "column name" }];

	options.forEach((optionData) => {
		let option = document.createElement("option");
		option.value = optionData.value;
		option.text = optionData.text;
		colName.appendChild(option);
	});

	addConditionBox.appendChild(colName);

	let condition = document.createElement("select");
	options = [
		{ value: "=", text: "=" },
		{ value: ">", text: ">" },
		{ value: ">=", text: ">=" },
		{ value: "<", text: "<" },
		{ value: "<=", text: "<=" },
	];

	options.forEach((optionData) => {
		let option = document.createElement("option");
		option.value = optionData.value;
		option.text = optionData.text;
		condition.appendChild(option);
	});

	addConditionBox.appendChild(condition);

	let value = document.createElement("input");
	value.placeholder = "Enter the value";

	addConditionBox.appendChild(value);

	let cancel = document.createElement("h2");
	cancel.textContent = "⨯";
	cancel.style.cursor = "pointer";
	cancel.onclick = function() {
		removeColumnInput(this.parentElement);
	};

	addConditionBox.appendChild(cancel);

	condition_container.appendChild(addConditionBox);
}

function addSelect() {
	console.log("Ithu select uh!!!");

	let queryOptions = document.getElementById("query-options");

	let selectedColumns = new Set(
		Array.from(queryOptions.getElementsByTagName("select")).map(
			(sel) => sel.value
		)
	);

	console.log(selectedColumns);

	let addConditionBox = document.createElement("select");

	// Default option
	// let defaultOption = document.createElement("option");
	// defaultOption.text = "Default Column";
	// defaultOption.value = "";
	// addConditionBox.appendChild(defaultOption);

	// Add all column names, skipping selected ones
	Object.values(allTable)
		.flat()
		.forEach((colName) => {
			if (!selectedColumns.has(colName)) {
				let option = document.createElement("option");
				option.value = colName;
				option.text = colName;
				addConditionBox.appendChild(option);
			}
		});

	queryOptions.appendChild(addConditionBox);
	queryOptions.appendChild(document.getElementById("addColumn"));

	let cancelColumnBtn = document.getElementById("cancelColumn");
	console.log(cancelColumnBtn);

	cancelColumnBtn.style.display = "flex";

	// Show cancel button if more than 1 select exists
	if (queryOptions.getElementsByTagName("select").length > 2) {
		queryOptions.appendChild(cancelColumnBtn);
	} else {
		cancelColumnBtn.style.display = "none";
	}
}

function addSelect() {
	console.log("Ithu select uh!!!");

	let queryOptions = document.getElementById("query-options");

	let selectedColumns = new Set(
		Array.from(queryOptions.getElementsByTagName("select")).map(
			(sel) => sel.value
		)
	);

	console.log(selectedColumns);

	let addSelectBox = document.createElement("select");

	// Get columns for the current table
	let columns = allTable[currentTable] || [];

	// Add column names, skipping selected ones
	columns.forEach((colName) => {
		if (!selectedColumns.has(colName)) {
			let option = document.createElement("option");
			option.value = colName;
			option.text = colName;
			addSelectBox.appendChild(option);
		}
	});

	console.log("Inga varthu");

	// Append only if there are available columns
	if (addSelectBox.options.length > 0) {
		queryOptions.appendChild(addSelectBox);
		queryOptions.appendChild(document.getElementById("addColumn"));
	}

	let cancelColumnBtn = document.getElementById("cancelColumn");
	console.log(cancelColumnBtn);

	// Show cancel button if more than 1 select exists
	if (queryOptions.getElementsByTagName("select").length > 2) {
		cancelColumnBtn.style.display = "flex";
		queryOptions.appendChild(cancelColumnBtn);
	} else {
		cancelColumnBtn.style.display = "none";
	}
}

function cancelSelect(btn) {
	console.log(btn);

	let selectedColumns = document
		.getElementById("query-options")
		.getElementsByTagName("select");

	console.log(selectedColumns);

	removeColumnInput(selectedColumns[selectedColumns.length - 1]);

	if (btn.parentElement.getElementsByTagName("select").length < 3) {
		console.log("wtfwtfwtfwtfwtfwtf");

		btn.style.display = "none";
	}
}

function updateAddColumn() {
	console.log("multiple column update called");

	let existingElements = Array.from(
		document.getElementById("query-options").getElementsByTagName("select")
	).filter((el) => getComputedStyle(el).display !== "none");

	console.log(existingElements); // This will contain only visible select elements

	for (let i = existingElements.length - 1; i >= 2; i--) {
		existingElements[i].remove();
	}

	let conditionBox = document.getElementById("condition_container");

	let input = document.createElement("input");
	input.placeholder = "Enter Column name";
	let datatype = document.createElement("select");
	let cons = document.createElement("select");

	datatypes.forEach((data) => {
		let option = document.createElement("option");
		option.value = data.value;
		option.text = data.text;
		datatype.appendChild(option);
	});

	constraints.forEach((con) => {
		let option = document.createElement("option");
		option.value = con.value;
		option.text = con.text;
		cons.appendChild(option);
	});

	cons.onchange = function() {
		if (this.value == "DEF") {
			let sibling = this.parentElement.parentElement;

			let defaultBox = document.createElement("div");
			defaultBox.className = "defaultBox";
			let txt = document.createElement("span");
			txt.textContent = "Enter the default value: ";
			defaultBox.appendChild(txt);

			let input = document.createElement("input");
			input.classList.add("input-box");

			//   input.type = "text";
			//   input.id = "default";

			defaultBox.appendChild(input);

			//   let sibling = this.parentElement.parentElement;
			//   console.log(parentTable);
			sibling.appendChild(defaultBox);
		} else if (this.value == "FK") {
			let siblings = this.parentElement.parentElement.childNodes;

			for (const element of siblings) {
				if (element.className == "defaultBox") {
					element.remove();
				}
			}

			let foreignAddress = document.createElement("div");
			foreignAddress.className = "foreignBox";

			let span = document.createElement("span");
			span.innerText = "References: ";

			// Table select dropdown
			let tableSelect = document.createElement("select");
			tableSelect.style.width = "30%";
			tableSelect.style.height = "100%";

			let defaultTableOption = document.createElement("option");
			defaultTableOption.value = "";
			defaultTableOption.textContent = "Select Table";
			tableSelect.appendChild(defaultTableOption);

			// Column select dropdown
			let colSelect = document.createElement("select");
			colSelect.style.width = "30%";
			colSelect.style.height = "100%";

			let defaultColOption = document.createElement("option");
			defaultColOption.value = "";
			defaultColOption.textContent = "Select Column";
			colSelect.appendChild(defaultColOption);

			// Populate table select with available tables
			let tableNames = Object.keys(allTable);
			if (tableNames.length > 0) {
				tableNames.forEach((table) => {
					let option = document.createElement("option");
					option.value = table;
					option.textContent = table;
					tableSelect.appendChild(option);
				});

				// Automatically select the first valid table and update columns
				tableSelect.value = tableNames[0];
				updateColumns(tableNames[0]);
			}

			// Function to update columns based on selected table
			function updateColumns(selectedTable) {
				colSelect.innerHTML = ""; // Clear previous options

				if (selectedTable && allTable[selectedTable]) {
					allTable[selectedTable].forEach((column) => {
						let option = document.createElement("option");
						option.value = column;
						option.textContent = column;
						colSelect.appendChild(option);
					});
				} else {
					let noCol = document.createElement("option");
					noCol.value = null;
					noCol.textContent = "No Columns available";
					colSelect.appendChild(noCol);
				}
			}

			// Set onchange event for table select
			tableSelect.onchange = function() {
				updateColumns(this.value);
			};

			foreignAddress.appendChild(span);
			foreignAddress.appendChild(tableSelect);
			foreignAddress.appendChild(colSelect);

			let parentTable = this.parentElement.parentElement;
			parentTable.appendChild(foreignAddress);
		}
	};

	let updateOptionHolder = document.createElement("div");

	updateOptionHolder.className = "updateOptionHolder";
	updateOptionHolder.style.border = "1px solid";

	let removeBtn = document.createElement("h3");
	removeBtn.innerText = "X";

	updateOptionHolder.appendChild(input);
	updateOptionHolder.appendChild(datatype);
	updateOptionHolder.appendChild(cons);
	updateOptionHolder.appendChild(removeBtn);

	let dynamicAlterAddColumn = document.createElement("div");
	dynamicAlterAddColumn.style.border = "1px solid green";
	dynamicAlterAddColumn.appendChild(updateOptionHolder);

	conditionBox.appendChild(dynamicAlterAddColumn);
}

function alterForeign() {
	// let conditionContainer = document.getElementById("condition_container");

	let defWhereBox = document.getElementById("where-container");

	// console.log(defWhereBox);

	// conditionContainer.innerHTML = "";
	defWhereBox.style.display = "none";

	// conditionContainer.appendChild(defWhereBox);

	let foreignAddress = document.createElement("div");
	foreignAddress.className = "foreignBox";

	let span = document.createElement("span");
	span.innerText = "References: ";

	///////////////////

	let tabeName = document.createElement("select");
	tabeName.style.width = "30%";
	tabeName.style.height = "100%";
	// select.style.overflow="visible";

	databases[currentDatabase].forEach((el) => {
		let option = document.createElement("option");
		option.value = el;
		option.textContent = el;
		tabeName.appendChild(option);
	});

	let colName = document.createElement("select");
	columnData.forEach((column) => {
	    column.constraints.forEach((constraint) => {
	        if (constraint.type === "PRIMARY_KEY") {
	            // console.log("Column '" + column.name + "' has a PRIMARY_KEY constraint.");
	            let option = document.createElement("option");
	            option.value = column.name;
	            option.textContent = column.name;
	            colName.appendChild(option);
	        }
	    });
	});


	colName.style.width = "30%";
	colName.style.height = "100%";

	foreignAddress.appendChild(span);
	foreignAddress.appendChild(tabeName);
	foreignAddress.appendChild(colName);

	// conditionContainer.appendChild(foreignAddress);
}

function alterDefault() {
	// let conditionContainer = document.getElementById("condition_container");

	let defWhereBox = document.getElementById("where-container");

	// conditionContainer.innerHTML = "";
	defWhereBox.style.display = "none";

	// conditionContainer.appendChild(defWhereBox);
	let defaultBox = document.createElement("div");
	defaultBox.className = "defaultBox";
	let txt = document.createElement("span");
	txt.textContent = "Enter the default value: ";
	defaultBox.appendChild(txt);

	let input = document.createElement("input");
	input.classList.add("input-box");

	input.type = "text";
	input.id = "default";

	defaultBox.appendChild(input);

	// conditionContainer.appendChild(defaultBox);
}
function addCons(element) {
	let column = element.parentElement.previousElementSibling.value; // Identify the column
	let dynamicTable = element.parentElement.parentElement.parentElement;

	let consHolder = document.createElement("div");
	consHolder.classList.add("consHolder");

	let constraint = document.createElement("select");
	constraint.classList.add("consClass");

	let extraConstraintBoxes = dynamicTable.getElementsByClassName("consHolder"); // Constraints specific to this column
	let chosen = [];

	// Store selected constraints only for this column
	chosen.push(column);

	for (const el of extraConstraintBoxes) {
		chosen.push(el.childNodes[0].value);
	}

	for (const opt of constraints) {
		if (!chosen.includes(opt.value)) {
			const option = document.createElement("option");
			option.value = opt.value;
			option.textContent = opt.text;
			constraint.appendChild(option);
		}
	}

	constraint.onchange = function() {
		let extraConstraint = dynamicTable.querySelectorAll(".consHolder");
		if (extraConstraint.length > 1) {
			let lastExtraConstraint = extraConstraint[extraConstraint.length - 1];
			if (lastExtraConstraint.childNodes[0].value == constraint.value) {
				removeColumnInput(lastExtraConstraint);
			}
		}
		dynamicConstraint(this);
	};

	consHolder.appendChild(constraint);
	dynamicTable.appendChild(consHolder);

	let totalConstraints = dynamicTable.querySelectorAll(".consClass").length;
	element.style.display = totalConstraints > 2 ? "none" : "flex";
	element.parentElement.lastElementChild.style.display = "flex";
}

function removeCons(element) {
	console.log(element);

	console.log(document.getElementById("addCons"));

	console.log(
		element.parentElement.parentElement.parentElement.querySelectorAll(
			".consHolder"
		).length
	);

	// let consBoxes = document.querySelectorAll(".consClass");
	let consBoxes =
		element.parentElement.parentElement.parentElement.querySelectorAll(
			".consHolder"
		);
	console.log(consBoxes);
	// removeColumnInput(consBoxes[consBoxes.length - 1].parentElement);
	removeColumnInput(consBoxes[consBoxes.length - 1]);

	if (consBoxes.length < 2) {
		// document.getElementById("removeCons").style.display = "none";
		element.style.display = "none";
	} else {
		// document.getElementById("addCons").style.display = "flex";
		console.log("Ulla varthu");

		console.log(element.parentElement.childNodes);

		element.parentElement.firstChild.style.display = "flex";
	}

	// document.remove(consBoxes[consBoxes.length-1]);
}

function dynamicConstraint(element) {
	let parent = element.parentElement;
	let firstChild = parent.firstElementChild; // Get the first element child

	if (element.value == "DEF") {
		parent.innerHTML = ""; // Clears all children, including firstChild
		let txt = document.createElement("span");
		txt.innerText = "Enter the default value: ";
		let input = document.createElement("input");
		input.placeholder = "Default value"; // Fix: Correct placeholder assignment

		parent.appendChild(firstChild); // Re-add the first child
		parent.appendChild(txt);
		parent.appendChild(input);
	} else if (element.value == "FK") {
		parent.innerHTML = ""; // Clears all children, including firstChild
		let txt = document.createElement("span");
		txt.innerText = "References: ";

		// console.log("jnjfnvjjkfvbvkdvjdjkvvkfvfkf");

		let table = document.createElement("select");

		databases[currentDatabase].forEach((el) => {
			let option = document.createElement("option");
			option.text = el;
			option.value = el;
			table.appendChild(option);
		});

		let column = document.createElement("select");

		//Foreign Column for the preloaded table
		length;
		let initialTable = table.value;
		if (
			allTable[initialTable].length > 0 &&
			allTable[initialTable][0] != null
		) {
			console.log("gceuygagyucayuuyucf");
			let option = document.createElement("option");
			option.value = allTable[initialTable][0];
			option.text = allTable[initialTable][0];
			column.appendChild(option);
		} else {
		}

		table.onchange = function() {
			let chosenTable = this.value;
			console.log(chosenTable);

			console.log(chosenTable);
			console.log(allTable);

			if (
				allTable[chosenTable].length > 0 &&
				allTable[chosenTable][0] != null
			) {
				let newColumns = document.createElement("select");

				console.log("gceuygagyucayuuyucf");
				let option = document.createElement("option");
				option.value = allTable[chosenTable][0];
				option.text = allTable[chosenTable][0];
				newColumns.appendChild(option);

				table.parentElement.replaceChild(
					newColumns,
					table.parentElement.lastElementChild
				);
			} else {
				// Update the foreign columns
				let newColumns = document.createElement("select");
				table.parentElement.replaceChild(
					newColumns,
					table.parentElement.lastElementChild
				);
			}
		};

		parent.appendChild(firstChild); // Re-add the first child
		parent.appendChild(txt);
		parent.appendChild(table);
		parent.appendChild(column);

		// console.log(table.value);
	} else {
		console.log("You can");

		let parent = element.parentElement;
		let firstChild = parent.firstElementChild;
		parent.innerHTML = "";
		parent.appendChild(firstChild);
	}
}

function deleteDB() {
	// Create modal container
	let modal = document.createElement("div");
	modal.id = "deleteModal";
	modal.style.position = "fixed";
	modal.style.top = "0";
	modal.style.left = "0";
	modal.style.width = "100%";
	modal.style.height = "100%";
	modal.style.background = "rgba(0, 0, 0, 0.5)";
	modal.style.display = "flex";
	modal.style.justifyContent = "center";
	modal.style.alignItems = "center";
	modal.style.zIndex = "1000";

	// Create modal content
	let modalContent = document.createElement("div");
	modalContent.classList.add("modalContent");

	// Create text
	let text = document.createElement("p");
	text.innerText = "Do you really want to delete it?";

	// Create Yes button
	let yesButton = document.createElement("button");
	yesButton.innerText = "Yes";
	yesButton.classList.add("yesButton");

	yesButton.onclick = async function() {
		let db = {
			dbName: currentDatabase,
			action: "dropDb",
		};

		dropFetch(db);

		document.body.removeChild(modal);
	};

	// Create No button
	let noButton = document.createElement("button");
	noButton.innerText = "No";
	noButton.classList.add("noButton");

	noButton.onclick = function() {
		document.body.removeChild(modal);
	};

	// Append elements
	modalContent.appendChild(text);
	modalContent.appendChild(yesButton);
	modalContent.appendChild(noButton);
	modal.appendChild(modalContent);
	document.body.appendChild(modal);
}

function selectTable(element) {
	if (element == null) {
		document.querySelectorAll(".selectedTable").forEach((el) => {
			el.classList.remove("selectedTable");
		});
		return;
	}

	currentTable = element.querySelector("span").innerText;

	// console.log(element.parentElement);

	document.querySelectorAll(".table-item").forEach((el) => {
		// console.log(el.querySelector("span").innerText);

		if (el.querySelector("span").innerText != currentTable) {
			el.classList.remove("selectedTable");
		} else {
			element.classList.add("selectedTable");
		}
	});
}

function deleteTable() {
	// Create modal container
	let modal = document.createElement("div");
	modal.style.position = "fixed";
	modal.style.top = "0";
	modal.style.left = "0";
	modal.style.width = "100%";
	modal.style.height = "100%";
	modal.style.background = "rgba(0, 0, 0, 0.5)";
	modal.style.display = "flex";
	modal.style.justifyContent = "center";
	modal.style.alignItems = "center";
	modal.style.zIndex = "1000";

	// Create modal content
	let modalContent = document.createElement("div");
	modalContent.classList.add("modalContent");

	// Create text
	let text = document.createElement("p");
	text.innerText = "Do you really want to delete it?";
	text.style.color = "#fff";

	// Create Yes button
	let yesButton = document.createElement("button");
	yesButton.innerText = "Yes";

	yesButton.classList.add("yesButton");

	yesButton.onclick = async function() {
		let db = {
			dbName: currentDatabase,
			tableName: currentTable,
			action: "dropTable",
		};

		dropFetch(db);
		document.body.removeChild(modal);
	};

	// Create No button
	let noButton = document.createElement("button");
	noButton.innerText = "No";
	noButton.classList.add("noButton");

	noButton.onclick = function() {
		document.body.removeChild(modal);
	};

	// Append elements
	modalContent.appendChild(text);
	modalContent.appendChild(yesButton);
	modalContent.appendChild(noButton);
	modal.appendChild(modalContent);
	document.body.appendChild(modal);
}

//Query operation

function colAction(element) {
	console.log("Onchange called");

	let whereBox = document.getElementById("where-container");
	if (element.value == "INSERT") {
		console.log("insert query");
		whereBox.style.display = "none";

		console.log(document.getElementById("def-column"));

		document.getElementById("def-column").style.display = "none";

		let whereDiv = document.getElementById("where-container");
		let conditionBox = document.getElementById("condition_container");

		conditionBox.innerHTML = "";

		whereDiv.style.display = "none";
		conditionBox.appendChild(whereDiv);

		console.log(element.parentElement.childNodes);

		Array.from(element.parentElement.children).forEach((el) => {
			if (el.id !== "def-column" && el.id !== "operation") {
				if (
					el.id == "addColumn" ||
					el.id == "cancelColumn" ||
					el.id == "cancelUpdateColumn"
				) {
					el.style.display = "none";
				} else {
					el.remove();
				}
			}
		});

		console.log(element.parentElement.childNodes);

		let box = document.getElementById("query-options");

		columnData.forEach((el) => {
			let assignBox = document.createElement("div");
			assignBox.classList.add("insertColumn");
			let colName = document.createElement("span");
			colName.innerText = el["name"];

			assignBox.appendChild(colName);

			if (el.dataType == "BLOB") {
				let fileInput = document.createElement("input");
				fileInput.type = "file";
				fileInput.id = "imageInput";
				fileInput.accept = "image/*";
			} else {
				let value = document.createElement("input");
				assignBox.appendChild(value);
			}

			box.appendChild(assignBox);
		});
	} else if (element.value == "select") {
		document.getElementById("def-column").style.display = "flex";

		let whereDiv = document.getElementById("where-container");
		let conditionBox = document.getElementById("condition_container");

		conditionBox.innerHTML = "";

		whereDiv.style.display = "flex";
		conditionBox.appendChild(whereDiv);

		console.log(document.getElementById("query-options").children);
		// populateWhereColumn();

		const queryOptions = document.getElementById("query-options");
		const children = [...queryOptions.children]; // Convert to array to avoid live updates

		for (let i = children.length - 1; i >= 0; i--) {
			const element = children[i];

			if (element.id == "cancelUpdateColumn") {
				element.style.display = "none";
				continue;
			}

			if (element.id == "addColumn" || element.id == "cancelColumn") {
				element.style.display = "flex";
				continue;
			} else if (element.id === "operation" || element.id === "def-column") {
				continue;
			}

			element.remove(); // Remove all other elements
		}

		document.getElementById("cancelColumn").style.display = "none";

		let defSelect = document.getElementById("def-column");
		defSelect.innerHTML = "";

		console.log(allTable[currentTable]);

		columnData.forEach((el) => {
			let option = document.createElement("option");
			option.text = el.name;
			option.value = el.name;
			defSelect.appendChild(option);
		});

		console.log(defSelect);

		// document.getElementById("query-options").lastChild.style.display="none";
	} else if (element.value == "UPDATE") {
		// console.log("Update clicked");

		let whereDiv = document.getElementById("where-container");
		let conditionBox = document.getElementById("condition_container");

		conditionBox.innerHTML = "";

		whereDiv.style.display = "flex";
		conditionBox.appendChild(whereDiv);

		whereBox.style.display = "flex";
		// populateWhereColumn();

		let elements = document.getElementById("query-options").children;
		console.log(elements);

		let originalDiv = document.getElementById("def-column");
		let clonedDiv = document.createElement("select");

		let addCondition = document.getElementById("addColumn");
		addCondition.style.display = "flex";
		console.log(addCondition);

		let addColumn = addCondition.cloneNode(true);
		addColumn.id = "addUpdate";

		for (let i = elements.length - 1; i >= 0; i--) {
			let element = elements[i];
			console.log(element);

			if (element.id == "operation") {
				continue;
			} else if (
				element.id == "cancelColumn" ||
				element.id == "def-column" ||
				element.id == "cancelUpdateColumn" ||
				element.id == "addColumn"
			) {
				element.style.display = "none";
				continue;
			} else {
				element.remove();
			}
		}

		// document.getElementById("cancelUpdateColumn").style.display = "flex";

		console.log(document.getElementById("cancelColumn"));

		//
		let equal = document.createElement("div");
		equal.classList.add("setEqual");
		let txt = document.createElement("h3");
		txt.textContent = "=";
		equal.appendChild(txt);
		//

		let assignBox = document.createElement("div");
		assignBox.classList.add("addUpdateColumn");

		// console.log(originalDiv);

		let valueInput = document.createElement("input");

		assignBox.appendChild(clonedDiv);
		assignBox.appendChild(equal);
		assignBox.appendChild(valueInput);

		let queryBox = document.getElementById("query-options");

		console.log(assignBox);

		function updateUpdateColumnOptions() {
			let selects = document.querySelectorAll(".updateColumnSelect");
			let selectedValues = new Set();

			// Collect all selected values
			selects.forEach((select) => {
				if (select.value) selectedValues.add(select.value);
			});

			// Update all select elements
			selects.forEach((select) => {
				Array.from(select.options).forEach((option) => {
					option.disabled =
						selectedValues.has(option.value) && option.value !== select.value;
				});
			});
		}

		// Function to create a new select element with column options
		function createColumnSelect() {
			let column = document.createElement("select");
			column.classList.add("updateColumnSelect");

			// Populate options from columnData
			columnData.forEach((col) => {
				let option = document.createElement("option");
				option.value = col.name;
				option.textContent = col.name;
				console.log(col);

				column.appendChild(option);
			});

			// Attach event listener
			column.addEventListener("change", updateUpdateColumnOptions);
			updateUpdateColumnOptions(); // Call immediately

			return column;
		}

		// Example usage in addColumn.onclick
		addColumn.onclick = function() {
			console.log("Adding new update column...");

			let newUpdateColumnBox = document.createElement("div");
			newUpdateColumnBox.classList.add("addUpdateColumn");

			let column = createColumnSelect();

			let equal = document.createElement("div");
			equal.classList.add("setEqual");
			let txt = document.createElement("h3");
			txt.textContent = "=";
			equal.appendChild(txt);

			let inputValue = document.createElement("input");

			newUpdateColumnBox.appendChild(column);
			newUpdateColumnBox.appendChild(equal);
			newUpdateColumnBox.appendChild(inputValue);
			queryBox.appendChild(newUpdateColumnBox);
			queryBox.appendChild(this);

			let removeBtn = document.getElementById("cancelUpdateColumn");

			//Minus button for update
			// if (!queryBox.contains(removeBtn)) {
			queryBox.appendChild(removeBtn);
			// }
			removeBtn.style.display = "flex";
		};

		// Prevent duplicate selections
		function updateUpdateColumnOptions() {
			let selects = document.querySelectorAll(".updateColumnSelect");
			let selectedValues = new Set();

			selects.forEach((select) => {
				if (select.value) selectedValues.add(select.value);
			});

			selects.forEach((select) => {
				Array.from(select.options).forEach((option) => {
					option.disabled =
						selectedValues.has(option.value) && option.value !== select.value;
				});
			});
		}

		// Attach event listener to handle existing selects
		document.addEventListener("change", function(event) {
			if (event.target.classList.contains("updateColumnSelect")) {
				updateUpdateColumnOptions();
			}
		});

		// queryBox.appendChild(assignBox);
		queryBox.appendChild(addColumn);
	} else if (element.value == "DELETE") {
		let whereDiv = document.getElementById("where-container");
		let conditionBox = document.getElementById("condition_container");

		conditionBox.innerHTML = "";

		whereDiv.style.display = "flex";
		conditionBox.appendChild(whereDiv);

		whereBox.style.display = "flex";

		let elements = document.getElementById("query-options").children;

		for (let i = elements.length - 1; i >= 0; i--) {
			let element = elements[i];

			if (element.id == "operation") {
				continue; // Keep only the operation dropdown
			} else if (
				element.id == "def-column" ||
				element.id == "addColumn" ||
				element.id == "cancelUpdateColumn"
			) {
				element.style.display = "none"; // Hide + and - buttons and def-column
				continue;
			} else {
				element.remove(); // Remove all other elements
			}
		}
	} else if (element.value == "ALTER") {
		// console.log("Alter clicked");

		let elements = document.getElementById("query-options").children;
		console.log(elements);

		for (let i = elements.length - 1; i >= 0; i--) {
			let element = elements[i];

			if (element.id == "operation") {
				continue;
			} else if (
				element.id == "def-column" ||
				element.id == "addColumn" ||
				element.id == "cancelUpdateColumn" ||
				element.id == "cancelColumn"
			) {
				element.style.display = "none";
				continue;
			} else {
				element.remove();
			}
		}

		let alter = document.createElement("select");
		alter.id = "alterAction";
		let options = [
			{ value: "DROPCOL", text: "Drop Column" },
			{ value: "ADDCOL", text: "Add Column" },
			{ value: "CHANGE", text: "Change Column" },
			{ value: "RENAME", text: "Rename Column" },
			{ value: "ADDCONS", text: "Add Constraint" },
			{ value: "DROPCONS", text: "Drop Constraint" },
		];

		alter.style.border = "1px solid";

		alter.onchange = function() {
			//   console.log("onchange aachum work aaguthe");
			//   console.log(this.value);

			if (this.value == "ADDCOL") {
				console.log("if kulla varthu");

				updateAddColumn();
			} else if (this.value == "CHANGE") {
				console.log("Change ulla vanthuttu");

				updateChangeColumn();
			} else if (this.value == "RENAME") {
				updateRename();
			} else if (this.value == "DROPCOL") {
				console.log("Ulla varthu man!!!");

				updateDropColumn();
				// document.getElementById("condition_container").innerHTML = "";
			} else if (this.value == "ADDCONS") {
				console.log("Alter add constraint called");
				updateAddCons();
			} else if (this.value == "DROPCONS") {
				console.log("Alter remove constraint called");

				updateRemoveCons();
			}
		};

		options.forEach((optionData) => {
			let option = document.createElement("option");
			option.value = optionData.value;
			option.text = optionData.text;
			alter.appendChild(option);
		});

		let dynamicAlterBox = document.createElement("div");
		dynamicAlterBox.style.border = "1px solid red";
		dynamicAlterBox.classList.add("dynamicAlterBox");

		// dynamicAlterBox.appendChild(column);
		// dynamicAlterBox.appendChild(alter);

		document.getElementById("query-options").appendChild(alter);

		let initialColumns = document.createElement("select");
		initialColumns.classList.add("dropColumnOption");

		columnData.forEach((el) => {
			let option = document.createElement("option");
			console.log(el.name);

			option.value = el.name;
			option.text = el.name;
			initialColumns.appendChild(option);
		});

		document.getElementById("query-options").appendChild(initialColumns);

		whereBox.style.display = "none";
	} else {
		console.log("select clicked");

		whereBox.style.display = "flex";
	}
}

function executeQuery() {
	console.log("Pannirlaam, Namma thaan!!!");

	let operation = document.getElementById("operation");
	if (operation.value == "INSERT") {
		// console.log("kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk");

		let insertBoxes = document
			.getElementById("query-options")
			.querySelectorAll(".insertColumn");
		console.log(insertBoxes);

		if (insertBoxes.length > 0) {
			let insertObject = {};
			insertBoxes.forEach(async (input) => {
				// console.log(input);

				let columnName = input.getElementsByTagName("span")[0].innerText; // Get column name
				let inputElement = input.getElementsByTagName("input")[0];
				let value = "";
				// console.log(value);

				if (inputElement.type == "text") {
					value = inputElement.value;
					console.log(value);
				} else if (inputElement.type == "file") {
					let file = inputElement.files[0];
					if (!file) {
						inputElement.value = "";
					} else {
						console.log("Yes....., You can");

						try {
							const base64String = await convertToBase64(file);
							value = base64String;
							console.log("Base64 Encoded Image:", base64String);
						} catch (error) {
							console.error("Error:", error);
						}
					}
				}

				insertObject[columnName] = value; // Assign input value
			});
			let finalinsertObject = {
				dbName: currentDatabase,
				tableName: currentTable,
				values: insertObject,
			};
			insertFetch(finalinsertObject);
			console.log(finalinsertObject);
		}
	} else if (operation.value == "select") {
		console.log("sbuwcbuubevuyvweyevuewvewyueuygvy");
		let selectedColumns =
			operation.parentElement.getElementsByTagName("SELECT");
		console.log(selectedColumns);

		let selectObject = {};

		let columns = [];
		let conditions = [];
		Array.from(selectedColumns)
			.slice(1)
			.forEach((col) => {
				columns.push(col.value);
			});

		let def_where = document.getElementById("where-container");
		console.log(def_where.getElementsByTagName("select")[0]);

		let colName = def_where.getElementsByTagName("select")[0].value;
		let condition = def_where.getElementsByTagName("select")[1].value;
		console.log(condition);

		let conditionValue = def_where.getElementsByTagName("input")[0].value;
		console.log(def_where.getElementsByTagName("input"));

		let extraCondition = document.querySelectorAll(".addConditionBox");
		let mandatoryCondition = {
			columnName: colName,
			conditionType: condition,
			conditionValue: conditionValue,
		};

		conditions.push(mandatoryCondition);

		console.log(extraCondition);

		if (extraCondition.length > 0) {
			extraCondition.forEach((el) => {
				let operator = el.getElementsByTagName("select")[0].value;
				let column = el.getElementsByTagName("select")[1].value;
				let condition = el.getElementsByTagName("select")[2].value;
				let conditionValue = el.getElementsByTagName("input")[0].value;

				console.log(el.getElementsByTagName("input"));

				let conditionObject = {
					operation: operator,
					columnName: column,
					conditionType: condition,
					conditionValue: conditionValue,
				};
				conditions.push(conditionObject);
			});
		}

		selectObject["DbName"] = currentDatabase;
		selectObject["tableName"] = currentTable;
		selectObject["columns"] = columns;
		selectObject["Conditions"] = conditions;

		def_where.getElementsByTagName("input")[0].value = "";

		console.log(selectObject);
	} else if (operation.value == "UPDATE") {
		let updatedColumns =
			operation.parentElement.querySelectorAll(".addUpdateColumn");
		console.log(updatedColumns);

		let updateObject = {};

		let conditions = [];
		let obj = {};
		updatedColumns.forEach((el) => {
			let colName = el.getElementsByTagName("select")[0].value;
			let value = el.getElementsByTagName("input")[0].value;
			obj[colName] = value;
		});

		/////conditions

		let def_where = document.getElementById("where-container");
		console.log(def_where.getElementsByTagName("select")[0]);

		let colName = def_where.getElementsByTagName("select")[0].value;
		let condition = def_where.getElementsByTagName("select")[1].value;
		console.log(condition);

		let conditionValue = def_where.getElementsByTagName("input")[0].value;
		console.log(def_where.getElementsByTagName("input"));

		let extraCondition = document.querySelectorAll(".addConditionBox");
		let mandatoryCondition = {
			columnName: colName,
			conditionType: condition,
			conditionValue: conditionValue,
		};

		conditions.push(mandatoryCondition);

		console.log(extraCondition);

		if (extraCondition.length > 0) {
			extraCondition.forEach((el) => {
				let operator = el.getElementsByTagName("select")[0].value;
				let column = el.getElementsByTagName("select")[1].value;
				let condition = el.getElementsByTagName("select")[2].value;
				let conditionValue = el.getElementsByTagName("input")[0].value;

				console.log(el.getElementsByTagName("input"));

				let conditionObject = {
					operation: operator,
					columnName: column,
					conditionType: condition,
					conditionValue: conditionValue,
				};
				conditions.push(conditionObject);
			});
		}

		updateObject["DbName"] = currentDatabase;
		updateObject["tableName"] = currentTable;
		updateObject["columnValue"] = obj;
		updateObject["conditions"] = conditions;

		console.log(updateObject);
	} else if (operation.value == "DELETE") {
		let deleteObject = {};

		let conditions = [];

		let def_where = document.getElementById("where-container");
		console.log(def_where.getElementsByTagName("select")[0]);

		let colName = def_where.getElementsByTagName("select")[0].value;
		let condition = def_where.getElementsByTagName("select")[1].value;
		console.log(condition);

		let conditionValue = def_where.getElementsByTagName("input")[0].value;
		console.log(def_where.getElementsByTagName("input"));

		let extraCondition = document.querySelectorAll(".addConditionBox");
		let mandatoryCondition = {
			columnName: colName,
			conditionType: condition,
			conditionValue: conditionValue,
		};

		conditions.push(mandatoryCondition);

		console.log(extraCondition);

		if (extraCondition.length > 0) {
			extraCondition.forEach((el) => {
				let operator = el.getElementsByTagName("select")[0].value;
				let column = el.getElementsByTagName("select")[1].value;
				let condition = el.getElementsByTagName("select")[2].value;
				let conditionValue = el.getElementsByTagName("input")[0].value;

				console.log(el.getElementsByTagName("input"));

				let conditionObject = {
					operation: operator,
					columnName: column,
					conditionType: condition,
					conditionValue: conditionValue,
				};
				conditions.push(conditionObject);
			});
		}

		deleteObject["DbName"] = currentDatabase;
		deleteObject["tableName"] = currentTable;
		deleteObject["conditions"] = conditions;

		console.log(deleteObject);
	} else if (operation.value == "ALTER") {
		// console.log("BDJBCJHVY");
		let alterObject;

		let alterAction = document.getElementById("alterAction").value;

		console.log(alterAction.value);
		let visibleElements = [];
		let queryBox = document.getElementById("query-options");
		for (const el of queryBox.children) {
			if (el.style.display !== "none") {
				visibleElements.push(el);
			}
		}

		if (alterAction == "DROPCOL") {
			alterObject = {
				action: "DROP_COLUMN",
				dbName: currentDatabase,
				tableName: currentTable,
				columnName: visibleElements[2].value,
			};
		} else if (alterAction == "ADDCOL") {
			console.log("innoonoio");

			let newColumn = document.querySelector(".updateOptionHolder").children;
			alterObject = {
				action: "ADD_COLUMN",
				dbName: currentDatabase,
				tableName: currentTable,
				columnName: newColumn[0].value,
				dataType: newColumn[1].value,
				constraints: {}, // Initialize constraints as an empty object
			};

			// Assign constraint type as a key with an empty object
			alterObject.constraints[newColumn[2].value] = {};

			if (newColumn[2].value == "FK") {
				let fkAddress = document.querySelector(".foreignBox");

				alterObject.constraints.FK.fkTable =
					fkAddress.firstChild.nextSibling.value;
				alterObject.constraints.FK.fkColumn = fkAddress.lastChild.value;
			} else if (newColumn[2].value == "DEF") {
				alterObject.constraints.DEF.defValue =
					document.querySelector(".defaultBox").lastChild.value;
			}

			console.log(alterObject);
		} else if (alterAction == "CHANGE") {
			let changeBox = document.getElementById("alterChangeHolder");
			alterObject = {
				dbName: currentDatabase,
				tableName: currentTable,
				columnName: changeBox.firstChild.value,
				dataType: changeBox.lastChild.value,
			};
		} else if (alterAction == "RENAME") {
			let renameHolder = document.getElementById("updateRenameHolder");
			console.log(renameHolder.childNodes);
			alterObject = {
				dbName: currentDatabase,
				tableName: currentTable,
				oldColumnName: renameHolder.firstChild.value,
				newColumnName: renameHolder.lastChild.value,
			};
		} else if (alterAction == "ADDCONS") {
			console.log(visibleElements);
			alterObject = {
				dbName: currentDatabase,
				tableName: currentTable,
				oldColumnName: visibleElements[2].value,
				newColumnName: visibleElements[3].value,
			};
		} else if (alterAction == "DROPCONS") {
			alterObject = {
				dbName: currentDatabase,
				tableName: currentTable,
				columnName: visibleElements[2].value,
				constraintName: visibleElements[3].value,
			};
		}

		console.log(alterObject);
	}
}

function cancelUpdateSelect(element) {
	console.log("ufbeiufweiufiew");

	let existingContainer =
		element.parentElement.querySelectorAll(".addUpdateColumn");
	console.log(existingContainer);
	console.log(existingContainer.length);

	if (existingContainer.length > 1) {
		removeColumnInput(existingContainer[existingContainer.length - 1]);
		element.parentElement.appendChild(element);
		// return;
	}

	if (!existingContainer.length > 1) {
		element.style.display = "none";
		console.log("ugxuyguyucy");
	}

	if (existingContainer.length < 2) {
		// console.log("WHY ULLA VARLA");
		// element.style.display = "none";
	}
	// element.parentElement.appendChild(element);
}

function printQuery() {
	// let outputDiv = document.querySelector(".tableDisplay");
	// outputDiv.innerHTML = "";
	// if (!Array.isArray(selectAllValues)) {
	//   console.error("selectAllValues is not an array:", selectAllValues);
	//   return; // Prevents further execution
	// }
	// let maxLength = Math.max(...selectAllValues.map((subArr) => subArr.length));
	// for (let i = 0; i < maxLength; i++) {
	//   let line = [];
	//   for (let j = 0; j < selectAllValues.length; j++) {
	//     if (selectAllValues[j][i] !== undefined) {
	//       line.push(selectAllValues[j][i]);
	//     }
	//   }
	//   let lineDiv = document.createElement("div");
	//   lineDiv.textContent = line.join(" "); // Add the line of text
	//   outputDiv.appendChild(lineDiv);
	// }
}


function updateChangeColumn() {
	// let conditionContainer = document.getElementById("condition_container");

	let defWhereBox = document.getElementById("where-container");

	// console.log(defWhereBox);

	// conditionContainer.innerHTML = "";
	defWhereBox.style.display = "none";
	// if (!conditionContainer.contains(defWhereBox)) {
	//   conditionContainer.appendChild(defWhereBox);
	// }

	console.log(defWhereBox);

	let queryOptionBox = document.getElementById("query-options");

	if (queryOptionBox.querySelectorAll(".dropColumnOption").length > 0) {
		queryOptionBox.querySelectorAll(".dropColumnOption")[0].remove();
	}

	let conditionBox = document.getElementById("condition_container");
	// conditionBox.style.display="flex";

	let outerBox = document.createElement("div");
	outerBox.style.display = "flex";
	outerBox.style.alignItems = "center";
	outerBox.style.justifyContent = "space-around";
	// outerBox.style.height="2em";
	// outerBox.style.width

	let txt = document.createElement("span");
	txt.innerText = "New data type: ";

	let colName = document.createElement("select");

	columnData.forEach((el) => {
		let option = document.createElement("option");
		option.text = el.name;
		option.value = el.name;
		colName.appendChild(option);
	});

	if (colName.options.length == 0) {
		let option = document.createElement("option");
		option.text = "No Columns available";
		option.value = null;
		colName.appendChild(option);
	}

	let selectDataType = document.createElement("select");

	datatypes.forEach((opt) => {
		const option = document.createElement("option");
		option.value = opt.value;
		option.textContent = opt.text;
		selectDataType.appendChild(option);
	});

	outerBox.appendChild(colName);
	outerBox.appendChild(txt);
	outerBox.appendChild(selectDataType);
	outerBox.id = "alterChangeHolder";

	conditionBox.appendChild(outerBox);
}

function updateRename() {
	// let conditionContainer = document.getElementById("condition_container");

	let defWhereBox = document.getElementById("where-container");

	// console.log(defWhereBox);
	if (document.getElementById("updateRenameHolder") != null)
		document.getElementById("updateRenameHolder").innerHTML = "";

	// conditionContainer.innerHTML = "";
	defWhereBox.style.display = "none";
	// if (!conditionContainer.contains(defWhereBox)) {
	//   conditionContainer.appendChild(defWhereBox);
	// }
	let queryOptionBox = document.getElementById("query-options");

	if (queryOptionBox.querySelectorAll(".dropColumnOption").length > 0) {
		queryOptionBox.querySelectorAll(".dropColumnOption")[0].remove();
	}

	let outerBox = document.createElement("div");
	outerBox.style.display = "flex";
	outerBox.style.alignItems = "center";
	outerBox.style.justifyContent = "space-around";

	let colName = document.createElement("select");

	columnData.forEach((el) => {
		let option = document.createElement("option");
		option.text = el.name;
		option.value = el.name;
		colName.appendChild(option);
	});

	let txt = document.createElement("span");
	txt.innerText = "Rename: ";

	let newName = document.createElement("input");
	newName.placeholder = "Rename";

	outerBox.appendChild(colName);
	outerBox.appendChild(txt);
	outerBox.appendChild(newName);
	outerBox.id = "updateRenameHolder";

	// conditionContainer.appendChild(outerBox);
}

function updateAddCons() {
	console.log(document.getElementById("where-container"));

	// let conditionContainer = document.getElementById("condition_container");

	let defWhereBox = document.getElementById("where-container");

	console.log(defWhereBox);

	// conditionContainer.innerHTML = "";
	// defWhereBox.style.display = "none";

	// if (!conditionContainer.contains(defWhereBox)) {
	//   conditionContainer.appendChild(defWhereBox);
	// }

	let queryBox = document.getElementById("query-options");

	let selects = queryBox.getElementsByTagName("select");

	for (let i = selects.length - 1; i >= 3; i--) {
		if (selects[i].id !== "operation" && selects[i].id !== "def-column") {
			queryBox.removeChild(selects[i]);
		}
	}

	console.log(queryBox.getElementsByTagName("select"));

	let colName = document.createElement("select");

	columnData.forEach((el) => {
		let option = document.createElement("option");
		option.value = el.name;
		option.text = el.name;
		colName.appendChild(option);
	});

	let queryOptionBox = document.getElementById("query-options");

	if (queryOptionBox.querySelectorAll(".dropColumnOption").length > 0) {
		queryOptionBox.querySelectorAll(".dropColumnOption")[0].remove();
	}

	let cons = document.createElement("select");

	for (const con of constraints) {
		if (con.value == "NONE") {
			continue;
		}

		let option = document.createElement("option");
		option.value = con.value;
		option.text = con.text;
		cons.appendChild(option);
	}

	cons.onchange = function() {
		if (this.value == "FK") {
			alterForeign();
		} else if (this.value == "DEF") {
			alterDefault();
		}
	};

	queryBox.appendChild(colName);
	queryBox.appendChild(cons);
}

function updateRemoveCons() {
	let queryBox = document.getElementById("query-options");
	let selects = queryBox.getElementsByTagName("select");

	console.log(selects);

	for (let i = selects.length - 1; i >= 3; i--) {
		if (selects[i].id !== "operation" && selects[i].id !== "def-column") {
			queryBox.removeChild(selects[i]);
		}
	}

	let colName = document.createElement("select");

	// Populate colName with column names from currentTable
	columnData.forEach((el) => {
		let option = document.createElement("option");
		option.value = el.name;
		option.text = el.name;
		colName.appendChild(option);
	});

	let cons = document.createElement("select");

	// Function to populate cons based on currentTable columns
	function populateCons() {
		cons.innerHTML = ""; // Clear previous options

		let hasConstraints = false;

		// Find the correct table in columnData
		let tableColumns = columnData.filter((col) => col.name === colName.value);

		tableColumns.forEach((column) => {
			if (column.constraints && column.constraints.length > 0) {
				hasConstraints = true;
				column.constraints.forEach((constraint) => {
					let option = document.createElement("option");
					option.value = constraint.type;
					option.text = constraint.type;
					cons.appendChild(option);
				});
			}
		});

		// If no constraints exist, show default message
		if (!hasConstraints) {
			let noCol = document.createElement("option");
			noCol.text = "No Constraints available";
			noCol.value = null;
			cons.appendChild(noCol);
		}
	}

	// Initial population of constraints
	populateCons();

	// Update constraints when column selection changes
	colName.onchange = populateCons;

	queryBox.appendChild(colName);
	queryBox.appendChild(cons);
}

function convertToBase64(file) {
	return new Promise((resolve, reject) => {
		const reader = new FileReader();

		reader.onload = function() {
			const base64String = reader.result.split(",")[1]; // Extract Base64 part
			resolve(base64String);
		};

		reader.onerror = function() {
			reject(new Error("Error reading file"));
		};

		reader.readAsDataURL(file); // Read file as Data URL
	});
}
// console.log(convertToBase64());

// // Toggle default value section
window.toggleDefaultSection = function(columnId) {
	const checkbox = document.getElementById(`${columnId}-default`);
	const section = document.getElementById(`${columnId}-default-section`);

	if (checkbox.checked) {
		section.style.display = "block";
	} else {
		section.style.display = "none";
	}
};

// Toggle foreign key section
window.toggleFKSection = function (columnId) {
  const checkbox = document.getElementById(`${columnId}-fk`);
  const section = document.getElementById(`${columnId}-fk-section`);

  if (checkbox.checked) {
    section.style.display = "block";
    let tablesSelect = section.getElementsByTagName("select")[0];

    tablesSelect.innerHTML="";

    Object.keys(allTable).forEach((key) => {
      if (allTable[key].length > 0 && allTable[key][0] != null) {
        let opt = document.createElement("option");
        opt.text = key;
        opt.value = key;
        tablesSelect.appendChild(opt);
      }
    });
    tablesSelect.onchange = function () {
      console.log(section.getElementsByTagName("select")[1]);
      section.getElementsByTagName("select")[1].innerHTML = "";

      console.log("VALUE: " + tablesSelect.value);
      let opt = document.createElement("option");
      opt.textContent = allTable[tablesSelect.value][0];
      opt.value = allTable[tablesSelect.value][0];

      console.log(opt);

      section.getElementsByTagName("select")[1].appendChild(opt);
    };
    tablesSelect.onchange();
  } else {
    section.style.display = "none";
  }
};

function loadPrimaryKeyData(id) {}

document.querySelectorAll(".navbar a").forEach((link) => {
  link.addEventListener("click", function (e) {
    e.preventDefault();
    document.querySelector(".navbar a.active").classList.remove("active");
    this.classList.add("active");
    operation(this);
  });
});

document.querySelectorAll(".navbar a").forEach((link) => {
	link.addEventListener("click", function(e) {
		e.preventDefault();
		document.querySelector(".navbar a.active").classList.remove("active");
		this.classList.add("active");
		operation(this);
	});
});

function operation(element) {
	console.log(element.id);

	// Removing the existing operator element
	let operationSpace = document.getElementById("operatorSpace");
	console.log(operationSpace.firstElementChild);
	console.log("hii");
	if (operationSpace.firstElementChild != null) {
		console.log("hii");
		console.log(operationSpace);

		operationSpace.firstElementChild.remove();
	}
	//removing the existing css
	document.querySelectorAll("link[rel='stylesheet']").forEach((link) => {
		if (
			!link.href.endsWith("dashboard.css") &&
			!link.href.endsWith("min.css")
		) {
			link.remove();
		}
	});

	//removing the existing script
	document.querySelectorAll("script").forEach((script) => {
		if (!script.src.endsWith("dashboard.js")) {
			script.remove();
		}
	});

	if (element.id == "createTableInDb") {
		$(document).ready(function() {
			$.get("create.html", function(response) {
				let content = $("<div>").html(response).find("#tableSection");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='create.css']").length) {
						//$("head").append('<link rel="stylesheet" href="create.css">');
						$("head").append(
							'<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">'
						);

						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'create.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);

					}
					// hello()
					// Load JS properly
					if (!$("script[src='create.js']").length) {
						let script = $('<script>', {
							type: 'text/javascript',
							src: 'create.js?v=' + new Date().getTime()  // Append a timestamp to force reloading
						});

						$('head').append(script);  // Append the <script> tag to the <head> section

					}

					content.show(); // Ensure it's visible
					console.log("Content appended successfully!");
				} else {
					console.log("Error: #selectContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	} else if (element.id == "operationSelect") {
		$(document).ready(function() {
			$.get("select.html", function(response) {
				let content = $("<div>").html(response).find("#selectContainer");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='select.css']").length) {
						$("head").append(
							'<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">'
						);

						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'select.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);
					}
					// hello()
					// Load JS properly
					if (!$("script[src='select.js']").length) {

						let script = $('<script>', {
							type: 'text/javascript',
							src: 'select.js?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(script);

					}
					content.show(); // Ensure it's visible
					console.log("Content appended successfully!");
				} else {
					console.log("Error: #selectContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	} else if (element.id == "operationDelete") {
		$(document).ready(function() {
			$.get("delete.html", function(response) {
				let content = $("<div>").html(response).find("#deleteContainer");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='delete.css']").length) {
						//$("head").append('<link rel="stylesheet" href="delete.css">');
						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'delete.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);
					}

					// hello()
					// Load JS properly
					if (!$("script[src='delete.js']").length) {

						let script = $('<script>', {
							type: 'text/javascript',
							src: 'delete.js?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(script);

					}

					content.show(); // Ensure it's visible
					console.log("Content appended successfully!");
				} else {
					console.log("Error: #deleteContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	} else if (element.id == "operationAlter") {
		$(document).ready(function() {
			$.get("alter.html", function(response) {
				let content = $("<div>").html(response).find("#alterContainer");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='alter.css']").length) {
						//$("head").append('<link rel="stylesheet" href="alter.css">');
						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'alter.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);
					}
					// hello()
					// Load JS properly
					if (!$("script[src='alter.js']").length) {

						let script = $('<script>', {
							type: 'text/javascript',
							src: 'alter.js?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(script);

					}

					content.show(); // Ensure it's visible
					console.log("Content appended successfully!!!!!!!!!!!!!!!!1111");
				} else {
					console.log("Error: #alterContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	} else if (element.id == "operationUpdate") {
		$(document).ready(function() {
			$.get("update.html", function(response) {
				let content = $("<div>").html(response).find("#updateContainer");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='update.css']").length) {
						//$("head").append('<link rel="stylesheet" href="update.css">');
						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'update.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);
					}
					// hello()
					// Load JS properly
					if (!$("script[src='update.js']").length) {

						let script = $('<script>', {
							type: 'text/javascript',
							src: 'update.js?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(script);

					}

					content.show(); // Ensure it's visible
					console.log("Content appended successfully!!!!!!!!!!!!!!!!1111");
				} else {
					console.log("Error: #updateContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	}
	if (element.id == "operationInsert") {
		$(document).ready(function() {
			$.get("insert.html", function(response) {
				let content = $("<div>").html(response).find("#insertContainer");

				if (content.length) {
					$("#operatorSpace").append(content);

					// Load CSS only if not already added
					if (!$("link[href='insert.css']").length) {
						//$("head").append('<link rel="stylesheet" href="insert.css">');
						let link = $('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: 'insert.css?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(link);
					}
					// hello()
					// Load JS properly
					if (!$("script[src='insert.js']").length) {

						let script = $('<script>', {
							type: 'text/javascript',
							src: 'insert.js?v=' + new Date().getTime()  // Append the timestamp as a query string
						});

						$('head').append(script);

					}

					content.show(); // Ensure it's visible
					console.log("Content appended successfully!!!!!!!!!!!");
					addRow();
				} else {
					console.log("Error: #insertContainer not found in dashboard.html");
				}
			}).fail(function(xhr) {
				console.log("Error loading content:", xhr.status, xhr.statusText);
			});
		});
	} else if (element.id == "noElement") {

	}
}

// operation(document.querySelector(".active"));

///fetch

async function createDatabaseFetch(createJSON) {
	console.log(dbName);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/CreateServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(createJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		const messageEl = document.getElementById("dbMessage");
		if (result.result) {
			console.log(databases);
			console.log("hello" + dbName);
			databases[createJSON.dbName] = [];
			console.log(databases);
			messageEl.textContent = "Database created successfully!";
			messageEl.classList.add("show");
			updateExplorer();
		} else {
			alert("Database creation failed!");
		}
	} catch (error) {
		console.error("Error:", error);
		alert("Failed to send data.");
	}
}



async function createFetch(createJSON) {
	console.log(dbName);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/CreateServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(createJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		const messageEl = document.getElementById("dbMessage");
		if (result.result) {
			databases[createJSON.dbName].push(createJSON.tableName)
			messageEl.textContent = "Table created successfully!";
			messageEl.classList.add("show");
			await tablePrimaryFetch();
			updateExplorer();
		} else {
			alert("Database creation failed!");
		}
	} catch (error) {
		console.error("Error:", error);
		alert("Failed to send data.");
	}
}

async function insertFetch(insertJSON) {
	console.log("hello", insertJSON);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/InsertServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(insertJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {
			console.log("Insert Success");
		} else {
			console.log("Failed")
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function updateServlet(updateJSON) {
	console.log(updateJSON);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/UpdateServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(updateJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {
			console.log("update success")
		} else {
			console.log("update failed")
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function deleteServlet(deleteJSON) {
	console.log(deleteJSON);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/DeleteServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(deleteJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {
			console.log("Delete success")
		} else {
			console.log("Failed to delete")
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function selectServlet(selectJSON) {
	console.log(selectJSON);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/SelectServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(selectJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {
			console.log("checkinggg..............", result.result)
			return result.result;
		} else {
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function selectAll() {
	let request = {
		dbName: currentDatabase,
		tableName: currentTable,
	};
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/SelectAllServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(request),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {
			selectAllValues = result.result;
			printQuery();
		} else {
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function alterServlet(alterJSON) {
	console.log(alterJSON);
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/AlterServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(alterJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		if (result.result) {

			console.log("ALTER SUCCESS")
			await tablePrimaryFetch();
		} else {
			console.log("ALTER FAILED")
		}
	} catch (error) {
		console.error("Error:", error);
	}
	fetchTableServlet();
}

async function fetchTableServlet() {
	console.log("cam");
	const requestData = {
		dbName: currentDatabase,
		tableName: currentTable,
	};
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/FetchTableData",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(requestData),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();
		columnData = result.columns;
		console.log("columnData", columnData);
	} catch (error) {
		console.error("Error:", error);
	}
}

function dropTable() {
	let db = { dbName: currentDatabase, tableName: currentTable, action: "dropTable" };
	dropFetch(db);
}

async function dropDb() {
	let db = { dbName: currentDatabase, action: "dropDb" };
	await dropFetch(db);
	//databases
	if (Object.keys(databases).length > 0) {
		console.log("drop and select database"); Object.keys(databases)
		console.log(Object.keys(databases));
		let dbName = Object.keys(databases)[0];
		console.log(dbName);
		let currentDBHeader = document.getElementById("dbExplorer").firstElementChild.firstElementChild;
		console.log(currentDBHeader)
		selectDatabase(dbName, currentDBHeader);
	} else {
		let operationSpace = document.getElementById("operatorSpace");
		console.log(operationSpace.firstElementChild);
		console.log("hii");
		if (operationSpace.firstElementChild != null) {
			console.log("hii");
			console.log(operationSpace);
			operationSpace.firstElementChild.remove();
		}
	}



}

async function dropFetch(dropJSON) {
	console.log(dropJSON)
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/DropServlet",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include",
				body: JSON.stringify(dropJSON),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const result = await response.json();

		if (result.result) {
			if (dropJSON.action == "dropTable") {
				let tables = databases[currentDatabase];
				tables.splice(tables.indexOf(currentTable), 1);
				await tablePrimaryFetch();
			}
			else if (dropJSON.action == "dropDb") {
				console.log("kjgfdsasdtyui")
				let db = dropJSON.dbName
				delete databases[db];

			}

			updateExplorer();
		}
	} catch (error) {
		console.error("Error:", error);
	}
}

async function tablePrimaryFetch() {
	const requestData = {
		dbName: currentDatabase,
	};
	try {
		const response = await fetch(
			"http://localhost:8080/Database/Service/FetchTablePrimaryKey",
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				credentials: "include", // Ensures cookies/sessions are sent
				body: JSON.stringify(requestData),
			}
		);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		const data = await response.json(); // Convert response to JSON
		allTable = data.columnData; // Extract columnData

	} catch (error) {
		console.error("Error:", error);
	}
}



