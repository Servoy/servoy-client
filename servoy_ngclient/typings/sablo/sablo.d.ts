/// <reference path="../angularjs/angular.d.ts" />

declare namespace sablo { 
	interface ILogService extends angular.ILogService {
		debugEnabled: boolean;
		debugLevel: number;
		DEBUG: number;
		SPAM: number;
	}
	interface FormState {
		model:any;
		layout:any;
		properties: any;
		style:any;
		initializing:boolean;
		initialDataRequested: boolean;
		getScope(): angular.IScope;			
	}
	interface WSSession {
		callService<T>(serviceName:string, methodName:string, argsObject, async:boolean):angular.IPromise<T>;
		sendMessageObject:()=>void;
		onopen:(handler:(evt)=>void)=>void;
		onerror:()=>void;
		onclose:()=>void;
		onMessageObject(handler:(msg, conversionInfo, scopesToDigest)=>void): void; 
	}
	interface Locale {
		language:string;
		country:string;
		full: string;
	}
	interface ISabloApplication {
        connect(context, queryArgs, websocketUri): WSSession;
        contributeFormResolver(contributedFormResolver:{prepareUnresolvedFormForUse(form:string)}): void;
        getSessionId(): string;
        getWindowName(): string;
        getWindowId(): string;
        getWindowUrl(): string;
        applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope:angular.IScope):void ;
        getComponentChanges(now, prev, beanConversionInfo, parentSize, property): any;
        getChangeNotifierGenerator(formName:string, beanName:string):(string);
        getFormState(name:string): angular.IPromise<FormState>;
        getFormStateWithData(name:string): angular.IPromise<FormState>;
        getFormStateEvenIfNotYetResolved(name:string): FormState;
        getFormStatesConversionInfo(): any;
		hasFormState(name:string): boolean;
        hasResolvedFormState(name:string): boolean;
        hasFormStateWithData(name:string):boolean;
        clearFormState(name:string): void;
        initFormState(formName, beanDatas, formProperties, formScope, resolve): FormState;
        updateScopeForState(formName: string, formScope: angular.IScope, state: FormState): FormState;
        resolveFormState(formName:string, skipTestResolving:boolean): FormState;
        unResolveFormState(formName:string): void;
        requestInitialData(formName:string, requestDataCallback:(initialFormData:any, formState:FormState)=>void): void;
        sendChanges(now, prev, formname:string, beanname:string, property:string): void;
        callService<T>(serviceName:string, methodName:string, argsObject, async:boolean): angular.IPromise<T>;
		callService<T>(serviceName:string, methodName:string, argsObject): angular.IPromise<T>;
        addToCurrentServiceCall(func:()=>void): void;
        getExecutor(formName:string):{on:(beanName:string, eventName:string, property:string, args, rowId:string)=>void};
        getLanguageAndCountryFromBrowser(): string;
        getLocale(): Locale;
        setLocale(locale:Locale): void;
        getCurrentFormUrl(fetch:boolean): string;
        setCurrentFormUrl(url:string, push:boolean): void;
	}

}