<!--
function filterTable(searchText, tableId, index) {
  searchText = searchText.toLowerCase();
  var table = $(tableId);
  for (var i = 1; i < table.rows.length; i++) {
    var tableRow = table.rows[i];
    var value = tableRow.cells[index].innerHTML;
        var displayStyle = '';
      if (value.toLowerCase().indexOf(searchText) < 0) {
      displayStyle = 'none';
      }
    table.rows[i].style.display = displayStyle;
  }
}

function switchFilter(filterId) {
  var filterLayer = $(filterId + "Layer");
  if (filterLayer.visible()) {
    filterLayer.hide();
    $(filterId).innerHTML = "Show Filter";
  } else {
    filterLayer.show();
    $(filterId).innerHTML = "Hide Filter";
  }
}
-->
