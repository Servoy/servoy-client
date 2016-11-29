/// <reference path="../angularjs/angular.d.ts" />

declare namespace servoy {
	interface IRootScopeService extends angular.IRootScopeService {
		updatingFormName: string;
		updatingFormUrl:string;		
	}
	
	interface SolutionSettings {
		solutionName:string,
		windowName:string,	
		mainForm: {name:string},
		navigatorForm: {name:string,size:{width:number}},
		solutionTitle: string,
		styleSheetPaths: string[],
		ltrOrientation : boolean,
		enableAnchoring: boolean,
		noLicense:{viewUrl:string,redirectUrl:string,redirectTimeout:number},
		maintenanceMode:{viewUrl:string,redirectUrl:string,redirectTimeout:number},
		sessionExpired:{viewUrl:string,redirectUrl:string},
		internalServerError:{viewUrl:string},		
	}
	
	interface IMainControllerScope extends angular.IScope {
		solutionSettings: SolutionSettings,
		getMainFormUrl():string,
		getNavigatorFormUrl():string,
		getSessionProblemView():string,
		getNavigatorStyle(ltrOrientation:string):any,
		getFormStyle(ltrOrientation):any
	}
} 