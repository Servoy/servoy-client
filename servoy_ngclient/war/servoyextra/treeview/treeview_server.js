/**
 * Sets the tree data
 * @param jsDataSet the JSDataSet used for the tree model
 * @example
 * 	var treeviewDataSet = databaseManager.createEmptyDataSet( 0,  ['id', 'pid', 'treeColumn', 'icon']);
 * 
 *	treeviewDataSet.addRow([1,		null,	'Main group',	'media:///group.png']);
 *	treeviewDataSet.addRow([2,		null,	'Second group',	'media:///group.png']);
 *	treeviewDataSet.addRow([3,		2,		'Subgroup',		'media:///group.png']);
 *	treeviewDataSet.addRow([4,		3,		'Mark',			'media:///user.png']);
 *	treeviewDataSet.addRow([5,		3,		'George',		'media:///user.png']);
 *
 *	%%prefix%%%%elementName%%.setDataSet(treeviewDataSet);
 */
$scope.api.setDataSet = function(jsDataSet) {
	$scope.model.jsDataSet = jsDataSet;
}