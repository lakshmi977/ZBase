window.onload = async function() {
	if (window.location.pathname.endsWith("update.html")) {
		window.location.href = "dashboard.html"; // Redirect if already logged in
	}
}

console.log("helllooo")


var columnSelect = document.querySelector(".update-column");
updateColumnDropdown(columnSelect);

updateColumnDropdown(document.querySelector(".condition-column"));

columnData.forEach((el) => {
	let option = document.createElement("option");
	option.text = el.name;
	option.value = el.name;
	document.querySelector(".condition-column").append(option);
});

function generateColumnOption() {
	let select = document.createElement("select");
	columnData.forEach((el) => {
		let option = document.createElement("option");
		option.text = el.name;
		option.value = el.name;
		select.append(option);
	});
	return select;
}

// Update column options in a dropdown
function updateColumnDropdown() {
	const selectedColumns = Array.from(document.querySelectorAll(".update-column"))
		.map(select => select.value)
		.filter(value => value); // Get selected columns (excluding empty ones)

	document.querySelectorAll(".update-column").forEach((dropdown) => {
		const currentValue = dropdown.value; // Store current selection
		dropdown.innerHTML = '<option value="">-- Select Column --</option>'; // Reset options

		columnData.forEach((column) => {
			if (!selectedColumns.includes(column.name) || column.name === currentValue) {
				const option = document.createElement("option");
				option.value = column.name;
				option.textContent = column.name;
				dropdown.appendChild(option);
			}
		});

		dropdown.value = currentValue; // Restore previous selection
	});
}

// Add a new update pair
function addUpdatePair() {
	const container = document.getElementById("update-container");

	const updatePair = document.createElement("div");
	updatePair.className = "update-pair";

	updatePair.innerHTML = `
        <select class="col-select update-column" onchange="updateColumnDropdown()">
            <option value="">-- Select Column --</option>
        </select>
        <input type="text" class="value-input" placeholder="New Value">
        <button class="btn-remove" onclick="removeUpdatePair(this)">&times;</button>
    `;

	container.appendChild(updatePair);
	updateColumnDropdown(); // Update all dropdowns to reflect the latest selections
}


// Remove an update pair
function removeUpdatePair(button) {
	const pair = button.parentElement;
	pair.remove();
} function addCondition() {

	const container = document.getElementById("conditions-container");

	const conditionDiv = document.createElement("div");
	conditionDiv.classList.add("condition");
	   conditionDiv.innerHTML = `
	    
	     <select class="operator-select">
	         <option value="=">=</option>
	         <option value="!=">!=</option>
	         <option value="<"><</option>
	         <option value=">">></option>
	         <option value=">=">>=</option>
	         <option value="<="><=</option>
	     </select>
	     <input type="text" class="value-input" placeholder="Value">
	     <button onclick="removeCondition(this)" class="btn-remove">&times;</button>
	 `;

	conditionDiv.prepend(generateColumnOption());

	container.appendChild(conditionDiv);

	// Update dropdown with column names
	const columnSelect = conditionDiv.querySelector(".condition-column");
	updateColumnDropdown(columnSelect);

	// Check if at least one condition exists, then add logical operator
	if (container.children.length > 0) {
		const logicalDiv = document.createElement("div");
		logicalDiv.classList.add("logical-operator"); // Add class to track later
		logicalDiv.innerHTML = `
            <select>
                <option>AND</option>
                <option>OR</option>
            </select>
        `;
		container.appendChild(logicalDiv); // Append logical operator before new condition
	}

	container.appendChild(conditionDiv);
}

function removeCondition(button) {
	const container = document.getElementById("conditions-container");
	const condition = button.parentElement;

	// Find the index of the condition
	const conditions = Array.from(container.getElementsByClassName("condition"));
	const index = conditions.indexOf(condition);

	// Remove the condition
	condition.remove();

	// Remove the corresponding logical operator
	if (index > 0) {
		// Remove the logical operator before this condition
		const logicalOperators =
			container.getElementsByClassName("logical-operator");
		if (logicalOperators[index - 1]) {
			logicalOperators[index - 1].remove();
		}
	} else if (container.children.length > 0) {
		// If the first condition is removed, remove the logical operator after it
		const firstLogicalOperator = container.querySelector(".logical-operator");
		if (firstLogicalOperator) {
			firstLogicalOperator.remove();
		}
	}
}

function executeQuery() {
	const updatePairs = document.querySelectorAll(".update-pair");
	const conditionPairs = document.querySelectorAll(".condition");
	const logicalOperator = "AND"; // Fixed logical operator for multiple conditions

	let updateData = {};
	let conditionsData = [];

	// Ensure at least one column to update is selected
	let hasUpdateColumn = false;

	updatePairs.forEach((pair) => {
		const column = pair.querySelector(".update-column").value;
		const value = pair.querySelector(".value-input").value;

		if (column) {
			updateData[column] = value;
			hasUpdateColumn = true;
		}
	});

	if (!hasUpdateColumn) {
		alert("Please select at least one column to update.");
		return;
	}

	// Collect conditions data
	conditionPairs.forEach((pair, index) => {
		const column = pair.querySelector(".condition-column").value;
		const operator = pair.querySelector(".operator-select").value;
		const value = pair.querySelector(".value-input").value;

		if (column) {
			let conditionObj = {
				column,
				operator,
				value,
			};

			// Add logicalOperator only from the second condition onward
			if (index > 0) {
				conditionObj.logicalOperator = logicalOperator;
			}

			conditionsData.push(conditionObj);
		}
	});

	// Warn if no conditions are provided
	if (conditionsData.length === 0) {
		if (
			!confirm(
				"Warning: You are about to update all records in the table. Continue?"
			)
		) {
			return;
		}
	}

	// Construct the final object
	const updateQueryObject = {
		dbName: currentDatabase,
		tableName: currentTable,
		columnValue: updateData,
		conditions: conditionsData,
	};

	console.log(updateQueryObject); // Output the object for testing
	updateServlet(updateQueryObject);
}
