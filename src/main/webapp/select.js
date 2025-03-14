window.onload = async function() {
	if (window.location.pathname.endsWith("select.html")) {
		window.location.href = "dashboard.html"; // Redirect if already logged in
	}
};

function startBuilderQuery() {
	console.log("hello");
	document.getElementById("start-container").style.display = "none";
	document.getElementById("selectContainer").style.display = "block";
}
globalColumnsList = []; // Declare a global list

namesList = [];
datatypeList = []; // New list for data types

function start(columnData) {
	const dataObj = JSON.parse(columnData);
	namesList = dataObj.columns.map((column) => column.name);
	datatypeList = dataObj.columns.map((column) => column.dataType); // Extract dataType for each column
	globalColumnsList = namesList;
	console.log("Names List:", namesList);
	console.log("Data Type List:", datatypeList);
}

function start(columnData) {
	if (!columnData) {
		console.error("columnData is null or undefined");
		return;
	}

	// let dataObj;
	// try {
	//   // Parse only if it's a string
	//   dataObj =
	//     typeof columnData === "string" ? JSON.parse(columnData) : columnData;
	// } catch (error) {
	//   console.error("Invalid JSON:", error);
	//   return;
	// }

	namesList = columnData.map((column) => column.name);
	datatypeList = columnData.map((column) => column.dataType);
	globalColumnsList = namesList;

	console.log("Names List:", namesList);
	console.log("Data Type List:", datatypeList);
}

start(columnData);

result = [];

orderDirection = "ASC"; // Default direction

function toggleOrder() {
	const upArrow = document.getElementById("upArrow");
	const downArrow = document.getElementById("downArrow");

	if (orderDirection === "ASC") {
		orderDirection = "DESC";
		upArrow.style.display = "none";
		downArrow.style.display = "inline";
	} else {
		orderDirection = "ASC";
		upArrow.style.display = "inline";
		downArrow.style.display = "none";
	}

	console.log("Order direction:", orderDirection);
}

function populateDropdown(selectElement, options) {
	options.forEach((name) => {
		const option = document.createElement("option");
		option.value = name;
		option.textContent = name;
		selectElement.appendChild(option);
	});
}

function removeOrder(element) {
	element.parentElement.remove();
}

var isOrderAdded = false; // Flag to track if order selection is already added


function toggleOrderDirection(button) {
	const upArrow = button.querySelector(".fa-sort-up");
	const downArrow = button.querySelector(".fa-sort-down");
	const orderInput = button
		.closest(".order-by-wrapper")
		.querySelector(".order-direction");

	if (upArrow.style.display === "none") {
		upArrow.style.display = "inline";
		downArrow.style.display = "none";
		let isOrderByAdded = false; // Prevent duplicate order by section

		function toggleOrderDirection(button) {
			const upArrow = button.querySelector(".fa-sort-up");
			const downArrow = button.querySelector(".fa-sort-down");
			const orderInput = button
				.closest(".order-by-wrapper")
				.querySelector(".order-direction");

			if (upArrow.style.display === "none") {
				upArrow.style.display = "inline";
				downArrow.style.display = "none";
				orderInput.value = "ASC";
			} else {
				upArrow.style.display = "none";
				downArrow.style.display = "inline";
				orderInput.value = "DESC";
			}

			console.log("Order direction set to:", orderInput.value);
		}
		erInput.value = "ASC";
	} else {
		upArrow.style.display = "none";
		downArrow.style.display = "inline";
		orderInput.value = "DESC";
	}

	console.log("Order direction set to:", orderInput.value);
}

function removeOrder(icon) {
	const wrapper = icon.parentElement;
	wrapper.remove();
	isOrderByAdded = false; // Reset flag when removed
}

// Function to remove an order-by row
function removeOrder(icon) {
	const wrapper = icon.parentElement;
	wrapper.remove();

	// If no order rows left, reset the flag and remove the "Order By" heading
	const container = document.getElementById("order-container");
	if (container.children.length === 1) {
		// Only the heading is left
		document.getElementById("orderByLabel").remove();
		isOrderByAdded = false; // Reset flag
	}
}

// Function to remove an order-by row
function removeOrder(icon) {
	icon.parentElement.remove();
}

// Function to populate checkboxes
function populateCheckboxes(containerId, options) {
	console.log("enter111");
	const container = document.getElementById(containerId);
	container.innerHTML = ""; // Clear existing checkboxes

	// Find max text length to set width dynamically
	const maxLength = Math.max(...options.map((name) => name.length));
	const maxWidth = maxLength * 10; // Adjust width based on text length

	// Update grid layout to accommodate longest text
	container.style.gridTemplateColumns = `repeat(auto-fill, minmax(${maxWidth}px, 1fr))`;

	options.forEach((name) => {
		const checkboxWrapper = document.createElement("div");
		checkboxWrapper.className = "checkbox-option";

		checkboxWrapper.innerHTML = `
                <input type="checkbox" id="col-${name}" name="columns" value="${name}">
                <label for="col-${name}" style="min-width: ${maxWidth}px;">${name}</label>
            `;

		container.appendChild(checkboxWrapper);
	});
}

// Populate checkboxes on page load
document.addEventListener("DOMContentLoaded", function() {
	start(columnData);
	console.log("enter");

	populateCheckboxes("checkbox-container", namesList);
});

// Function to populate dropdown options
function populateDropdown(selectElement, options) {
	selectElement.innerHTML = '<option value="">-- Select Column --</option>'; // Default option
	options.forEach((name) => {
		const option = document.createElement("option");
		option.value = name;
		option.textContent = name;
		selectElement.appendChild(option);
	});
}


start(columnData);
populateCheckboxes("checkbox-container", namesList);

// Function to add a new aggregate row
function addAggregate() {
	const container = document.getElementById("aggregate-container");

	// Check if the label already exists, if not, create it
	if (!document.getElementById("aggregateName")) {
		const aggregateLabel = document.createElement("p");
		aggregateLabel.id = "aggregateName";
		aggregateLabel.textContent = "Aggregate Function";
		aggregateLabel.style.color = "white"; // Styling
		aggregateLabel.style.fontWeight = "bold";
		aggregateLabel.style.marginBottom = "10px";
		aggregateLabel.style.marginRight = "10%";
		aggregateLabel.style.marginTop = "1%";

		container.prepend(aggregateLabel); // Add at the top
	}

	const aggregateWrapper = document.createElement("div");
	aggregateWrapper.className = "aggregate-wrapper";
	aggregateWrapper.innerHTML = `
    <select class="aggregate-function">
        <option value="">-- Select Function --</option>
        <option value="COUNT">COUNT</option>
        <option value="SUM">SUM</option>
        <option value="AVG">AVG</option>
        <option value="MIN">MIN</option>
        <option value="MAX">MAX</option>
    </select>
    <select class="aggregate-column"></select>
    <i class="fa-solid fa-xmark fa-rotate-90" style="color: #ffffff; cursor: pointer;" onclick="removeAggregate(this)"></i>
`;

	container.appendChild(aggregateWrapper);

	// Populate column dropdown with namesList
	const columnSelect = aggregateWrapper.querySelector(".aggregate-column");
	populateDropdown(columnSelect, namesList);
}

function removeAggregate(button) {
	button.parentElement.remove();
}
// Add Condition Row
function addCondition() {
	const conditionsContainer = document.getElementById("conditions-container");

	// Check if the label "Where Condition" already exists, if not, create it
	if (!document.getElementById("whereConditionLabel")) {
		const whereConditionLabel = document.createElement("p");
		whereConditionLabel.id = "whereConditionLabel";
		whereConditionLabel.textContent = "Where Condition";
		whereConditionLabel.style.color = "white"; // Styling
		whereConditionLabel.style.fontWeight = "bold";
		whereConditionLabel.style.marginBottom = "10px";
		whereConditionLabel.style.marginTop = "10px";

		conditionsContainer.prepend(whereConditionLabel); // Add label at the top
	}

	const conditionWrapper = document.createElement("div");
	conditionWrapper.className = "condition";
	conditionWrapper.innerHTML = `
<select class="logical-op">
        <option value="AND">AND</option>
        <option value="OR">OR</option>
    </select>
    <select class="col-select condition-column"></select>
    <select class="operator-select">
        <option value="=">=</option>
        <option value=">">></option>
        <option value="<"><</option>
        <option value=">=">>=</option>
        <option value="<="><=</option>
        <option value="<=">!=</option>
    </select>
    <input type="text" class="value-input" placeholder="Value">
    <i class="fa-solid fa-xmark fa-rotate-90" style="color: #ffffff; cursor: pointer;" onclick="removeCondition(this)"></i>
 
`;

	conditionsContainer.appendChild(conditionWrapper);

	// Populate column dropdown
	const columnSelect = conditionWrapper.querySelector(".condition-column");
	populateDropdown(columnSelect, namesList);

	// Hide AND/OR for the first row
	if (conditionsContainer.children.length === 2) {
		// Because the label counts as a child
		conditionWrapper.querySelector(".logical-op").style.display = "none";
	}
}

function removeCondition(button) {
	const conditionWrapper = button.parentElement;
	const conditionsContainer = document.getElementById("conditions-container");
	if (
		conditionWrapper.previousElementSibling === null &&
		conditionWrapper.nextElementSibling
	) {
		conditionWrapper.nextElementSibling.querySelector(
			".logical-op"
		).style.display = "none";
	}
	conditionWrapper.remove();
}

columns = [];
// Execute Query: Build query object, log it, and then switch view to the table

async function executeQuery() {
	const selectedColumns = Array.from(document.querySelectorAll('input[name="columns"]:checked'))
		.map(checkbox => checkbox.value);

	const aggregateFunctions = Array.from(document.querySelectorAll('.aggregate-wrapper'))
		.map(wrapper => {
			const func = wrapper.querySelector('.aggregate-function').value;
			const column = wrapper.querySelector('.aggregate-column').value;
			return func && column ? { column, function: func } : null;
		}).filter(Boolean);

	const invalidAggregate = aggregateFunctions.find(agg => !selectedColumns.includes(agg.column));
	if (invalidAggregate) {
		return;
	}

	const conditions = [];
	document.querySelectorAll('.condition').forEach((wrapper, index) => {
		const columnName = wrapper.querySelector('.condition-column').value;
		const conditionType = wrapper.querySelector('.operator-select').value;
		const conditionValue = wrapper.querySelector('.value-input').value;
		if (columnName && conditionType && conditionValue) {
			const conditionObj = { columnName, conditionType, conditionValue };
			if (index > 0) {
				conditionObj.operation = wrapper.querySelector('.logical-op').value;
			}
			conditions.push(conditionObj);
		}
	});

	// Instead of collecting an array for Order By, we now grab a single order object.
	const orderItem = document.querySelector('.order-item');
	let order = null;
	if (orderItem) {
		const orderColumn = orderItem.querySelector('.order-column').value;
		const orderDirection = orderItem.querySelector('.order-toggle').dataset.order;
		if (orderColumn !== "") {
			order = { column: orderColumn, direction: orderDirection };
		}
	}

	console.log("Selected Columns:", selectedColumns);
	console.log("Aggregate Functions:", aggregateFunctions);
	console.log("Order By:", order);

	if (aggregateFunctions.length > 0) {
		globalColumnsList = [...new Set(aggregateFunctions.map(agg => agg.column))];
	} else {
		globalColumnsList = [...selectedColumns];
	}

	const queryObject = {
		columns: selectedColumns,
		aggregate: aggregateFunctions,
		conditions: conditions,
		order: order  // Order is now an object rather than an array
	};

	console.log("Query Object:", queryObject);

	queryObject.dbName = currentDatabase;
	queryObject.tableName = currentTable;
	document.querySelector('.table-container').style.display = 'block';
	generateTable(await selectServlet(queryObject));
}

function generateTable(result) {
	// Check if result exists and has at least one column with data
	if (!result || result.length === 0 || result[0].length === 0) {
		const messageElem = document.getElementById("message");
		messageElem.style.display = "block";
		messageElem.innerText = "Invalid Query";
		messageElem.style.color = "red";
		messageElem.style.textAlign = "center";
		document.querySelector('.table-container').style.display = "none";
		return;
	}

	const messageElem = document.getElementById("message");
			messageElem.style.display = "none";
			messageElem.innerText = "Invalid Query";
			messageElem.style.color = "red";
			messageElem.style.textAlign = "center";
	// Show the table container
	document.querySelector('.table-container').style.display = "block";

	const tableHeader = document.getElementById('table-header');
	const tableBody = document.getElementById('table-body');

	// Clear previous header and body
	tableHeader.innerHTML = "";
	tableBody.innerHTML = "";

	const headerRow = document.createElement('tr');
	globalColumnsList.forEach(headerText => {
		const th = document.createElement('th');
		th.textContent = headerText;
		headerRow.appendChild(th);
	});
	tableHeader.appendChild(headerRow);

	// Determine number of rows and columns
	const numRows = result[0].length;  // Each sub-array represents a column
	const numColumns = result.length;  // Total number of columns

	// Build table rows from the column-oriented result array
	for (let i = 0; i < numRows; i++) {
		const row = document.createElement('tr');
		for (let j = 0; j < numColumns; j++) {
			const td = document.createElement('td');
			const cell = result[j][i];  // Notice the reversed indices: column j, then row i

			if (typeof cell === "string") {
				if (cell.startsWith("/9j/") || cell.startsWith("/+")) {  // JPEG
					td.innerHTML = `<img src="data:image/jpeg;base64,${cell}" width="50" height="50">`;
				} else if (cell.startsWith("iVBOR")) {  // PNG
					td.innerHTML = `<img src="data:image/png;base64,${cell}" width="50" height="50">`;
				} else if (cell.startsWith("R0lGOD")) {  // GIF
					td.innerHTML = `<img src="data:image/gif;base64,${cell}" width="50" height="50">`;
				} else if (cell.startsWith("PHN2Zy")) {  // SVG (Base64 encoded)
					td.innerHTML = `<img src="data:image/svg+xml;base64,${cell}" width="50" height="50">`;
				} else {
					td.textContent = cell;  // Normal string
				}
			} else {
				td.textContent = cell;  // Normal string (shorter than 20 characters)
			}

			row.appendChild(td);
		}
		tableBody.appendChild(row);
	}
}

function addOrderBy() {
    if (isOrderAdded) return; // Prevent multiple additions

    const orderList = document.getElementById("order-list");

    // Create order wrapper div
    const orderWrapper = document.createElement("div");
    orderWrapper.className = "order-item";

    // Create column dropdown
    const columnSelect = document.createElement("select");
    columnSelect.className = "order-column";
    columnSelect.innerHTML = `<option value="">-- Select Column --</option>` +
        globalColumnsList.map(col => `<option value="${col}">${col}</option>`).join("");

    // Create order direction toggle button (default: ASC)
    const orderToggle = document.createElement("button");
    orderToggle.className = "order-toggle";
    orderToggle.textContent = "▲"; // Default to ascending
    orderToggle.dataset.order = "ASC"; // Store order state

    orderToggle.addEventListener("click", function () {
        if (this.dataset.order === "ASC") {
            this.textContent = "▼";
            this.dataset.order = "DESC";
        } else {
            this.textContent = "▲";
            this.dataset.order = "ASC";
        }
    });

    // Remove button
    const removeButton = document.createElement("button");
    removeButton.className = "remove-order";
    removeButton.innerHTML = '<i class="fa-solid fa-xmark"></i>';
    removeButton.onclick = function () {
        orderWrapper.remove();
        isOrderAdded = false; // Reset flag when removed
    };

    // Append elements to order wrapper
    orderWrapper.appendChild(columnSelect);
    orderWrapper.appendChild(orderToggle);
    orderWrapper.appendChild(removeButton);
    orderList.appendChild(orderWrapper);

    isOrderAdded = true; // Set flag to prevent additional additions
}
