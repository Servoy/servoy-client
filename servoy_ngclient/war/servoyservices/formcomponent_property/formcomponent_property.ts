angular.module( 'formcomponent_property', ['webSocketModule'] )
	// Valuelist type -------------------------------------------
	.run( function( $sabloConverters: sablo.ISabloConverters, $typesRegistry: sablo.ITypesRegistryForSabloConverters, $componentTypeConstants ) {

		/** Initializes internal state on a new array value */
		function initializeNewValue( newValue ) {
			$sabloConverters.prepareInternalState( newValue );

			var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

			// implement what $sabloConverters need to make this work
			internalState.setChangeNotifier = function( changeNotifier ) {
				internalState.changeNotifier = changeNotifier;
			}
			internalState.isChanged = function() {
				var hasChanges = internalState.allChanged;
				return hasChanges;
			}

			// private impl
			internalState.allChanged = false;
		}

		function getChangeNotifier( propertyValue ) {
			return function() {
				var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
				internalState.changeNotifier();
			}
		}

		var formComponentPropertyType: sablo.IType<any> = {
			fromServerToClient: function(serverJSONValue: any, currentClientValue: any, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext) {
				let realValue = currentClientValue;
				if ( realValue == null ) {
					realValue = serverJSONValue;
					initializeNewValue( realValue );
				}
				
				if (serverJSONValue.childElements) {
					if (!realValue.childElements) realValue.childElements = [];
					else realValue.childElements.length = serverJSONValue.childElements.length;
						
					for ( let idx = 0; idx < serverJSONValue.childElements.length; idx++ ) {
						let childCompElem = serverJSONValue.childElements[idx];
	
						realValue.childElements[idx] = childCompElem = $sabloConverters.convertFromServerToClient( childCompElem, 
								$typesRegistry.getAlreadyRegisteredType($componentTypeConstants.CHILD_COMPONENT_TYPE_NAME), currentClientValue && currentClientValue.childElements ? currentClientValue.childElements[idx] : undefined,
								undefined, undefined, componentScope, propertyContext );
	
						if ( childCompElem && childCompElem[$sabloConverters.INTERNAL_IMPL] && childCompElem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier ) {
							// child is able to handle it's own change mechanism
							childCompElem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier( getChangeNotifier( realValue ) );
						}
					}
				}

				return realValue;
			},

			fromClientToServer: function( newClientData, oldClientData, scope: angular.IScope, propertyContext: sablo.IPropertyContext ) {
				if ( !newClientData ) return null;
				// only childElements are pushed.
				let changes = undefined;
				if (newClientData.childElements) {
					changes = [];
					for ( let idx = 0; idx < newClientData.childElements.length; idx++ ) {
						changes[idx] = $sabloConverters.convertFromClientToServer( newClientData.childElements[idx],
								$typesRegistry.getAlreadyRegisteredType($componentTypeConstants.CHILD_COMPONENT_TYPE_NAME), oldClientData && oldClientData.childElements ? oldClientData.childElements[idx] : null, scope, propertyContext );
					}
				}
				return changes;

			},

			updateAngularScope: function (clientValue: any, componentScope: angular.IScope): void {
				// nothing to do here
			}

		}
		$typesRegistry.registerGlobalType( 'formcomponent', formComponentPropertyType, false );
	} )
