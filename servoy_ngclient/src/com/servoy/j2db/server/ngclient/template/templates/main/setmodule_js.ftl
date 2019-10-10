<#ftl output_format="JavaScript">
if (typeof module === 'object') {
	<#-- Require Electron, IPC, other modules here -->
	window.module = module;
	module = undefined;
}