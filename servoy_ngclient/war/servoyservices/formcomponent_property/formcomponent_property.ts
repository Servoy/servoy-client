angular.module( 'formcomponent_property', ['webSocketModule'] )
	// Valuelist type -------------------------------------------
	.run( function( $sabloConverters, $sabloUtils, $q, $sabloTestability, $sabloApplication ) {

		/** Initializes internal state on a new array value */
		function initializeNewValue( newValue ) {
			var newInternalState = false; // TODO although unexpected (internal state to already be defined at this stage it can happen until SVY-8612 is implemented and property types change to use that
			if ( !newValue.hasOwnProperty( $sabloConverters.INTERNAL_IMPL ) ) {
				newInternalState = true;
				$sabloConverters.prepareInternalState( newValue );
			} // else: we don't try to redefine internal state if it's already defined

			var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

			if ( newInternalState ) {
				// implement what $sabloConverters need to make this work
				internalState.setChangeNotifier = function( changeNotifier ) {
					internalState.changeNotifier = changeNotifier;
				}
				internalState.isChanged = function() {
					var hasChanges = internalState.allChanged;
					//				if (!hasChanges) for (var x in internalState.changedIndexes) { hasChanges = true; break; }
					return hasChanges;
				}

				// private impl
				internalState.conversionInfo = [];
				internalState.allChanged = false;
			} // else don't reinitilize it - it's already initialized
		}

		function getChangeNotifier( propertyValue ) {
			return function() {
				var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
				internalState.changeNotifier();
			}
		}

		var formComponentProperty = {
			fromServerToClient: function( serverJSONValue, currentClientValue, componentScope, componentModelGetter ) {
				let conversionInfo = null;
				let realValue = currentClientValue;
				if ( realValue == null ) {
					realValue = serverJSONValue;
					initializeNewValue( realValue );
				}
				if ( serverJSONValue[$sabloConverters.TYPES_KEY] ) {
					conversionInfo = serverJSONValue[$sabloConverters.TYPES_KEY];
				}
				if ( conversionInfo ) {
					for ( let key in conversionInfo ) {
						let elem = serverJSONValue[key];

						const internalState = realValue[$sabloConverters.INTERNAL_IMPL];
						internalState.conversionInfo[key] = conversionInfo[key];
						realValue[key] = elem = $sabloConverters.convertFromServerToClient( elem, conversionInfo[key], currentClientValue ? currentClientValue[key] : undefined, componentScope, componentModelGetter );

						if ( elem && elem[$sabloConverters.INTERNAL_IMPL] && elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier ) {
							// child is able to handle it's own change mechanism
							elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier( getChangeNotifier( realValue ) );
						}
						if ( key == "childElements" && elem ) {
							for ( let i = 0; i < elem.length; i++ ) {
								var comp = elem[i];
								if ( comp && comp[$sabloConverters.INTERNAL_IMPL] && comp[$sabloConverters.INTERNAL_IMPL].setChangeNotifier ) {
									// child is able to handle it's own change mechanism
									comp[$sabloConverters.INTERNAL_IMPL].setChangeNotifier( getChangeNotifier( realValue ) );
								}
							}
						}
					}
				}
				return realValue;
			},

			fromClientToServer: function( newClientData, oldClientData ) {
				if ( !newClientData ) return null;
				// only childElements are pushed.
				const internalState = newClientData[$sabloConverters.INTERNAL_IMPL]
				let changes = $sabloConverters.convertFromClientToServer( newClientData["childElements"], internalState.conversionInfo["childElements"], oldClientData ? oldClientData["childElements"] : null );
				return changes;

			},

			updateAngularScope: function( clientValue, componentScope ) {
				// nothing to do here
			}

		}
		$sabloConverters.registerCustomPropertyHandler( 'formcomponent', formComponentProperty );
	} )
