AUI.add(
	'liferay-ddm-form-field-key-value',
	function(A) {
		var Lang = A.Lang;

		var KeyValueField = A.Component.create(
			{
				ATTRS: {
					generationLocked: {
						valueFn: '_valueGenerationLocked'
					},

					key: {
						valueFn: '_valueKey'
					},

					maxKeyInputSize: {
						value: 50
					},

					minKeyInputSize: {
						value: 5
					},

					strings: {
						value: {
							cancel: Liferay.Language.get('cancel'),
							done: Liferay.Language.get('done'),
							keyLabel: Liferay.Language.get('field-name')
						}
					},

					type: {
						value: 'key-value'
					}
				},

				EXTENDS: Liferay.DDM.Field.Text,

				NAME: 'liferay-ddm-form-field-key-value',

				prototype: {
					initializer: function() {
						var instance = this;

						instance._eventHandlers.push(
							instance.after('keyChange', instance._afterKeyChange),
							instance.bindContainerEvent('valuechange', instance._onValueChangeKeyInput, '.key-value-input'),
							instance.bindInputEvent('valuechange', instance._onValueChangeInput)
						);
					},

					getTemplateContext: function() {
						var instance = this;

						var key = instance.get('key');

						return A.merge(
							KeyValueField.superclass.getTemplateContext.apply(instance, arguments),
							{
								key: key,
								keyInputSize: instance._getKeyInputSize(key),
								strings: instance.get('strings'),
								tooltip: instance.getLocalizedValue(instance.get('tooltip'))
							}
						);
					},

					normalizeKey: function(key) {
						var instance = this;

						var normalizedKey = '';

						var nextUpperCase = false;

						key = key.trim();

						for (var i = 0; i < key.length; i++) {
							var item = key[i];

							if (item === ' ') {
								nextUpperCase = true;

								continue;
							}
							else if (!A.Text.Unicode.test(item, 'L') && !A.Text.Unicode.test(item, 'N')) {
								continue;
							}

							if (nextUpperCase) {
								item = item.toUpperCase();

								nextUpperCase = false;
							}

							normalizedKey += item;
						}

						return normalizedKey;
					},

					render: function() {
						var instance = this;

						var key = instance.get('key');

						if (!key) {
							instance.set('key', instance._valueKey());
						}

						KeyValueField.superclass.render.apply(instance, arguments);

						return instance;
					},

					_afterKeyChange: function(event) {
						var instance = this;

						instance.set('generationLocked', event.newVal !== instance.normalizeKey(instance.getValue()));

						instance._uiSetKey(event.newVal);
					},

					_getKeyInputSize: function(str) {
						var instance = this;

						var size = str.length;

						var maxKeyInputSize = instance.get('maxKeyInputSize');

						var minKeyInputSize = instance.get('minKeyInputSize');;

						if (size > maxKeyInputSize) {
							size = maxKeyInputSize;
						}
						else if (size <= minKeyInputSize) {
							size = minKeyInputSize;
						}

						return size + 1;
					},

					_onValueChangeInput: function(event) {
						var instance = this;

						if (!instance.get('generationLocked')) {
							var value = instance.getValue();

							instance.set('key', instance.normalizeKey(value));
						}
					},

					_onValueChangeKeyInput: function(event) {
						var instance = this;

						var value = event.newVal;

						instance.set('key', instance.normalizeKey(value));
					},

					_renderErrorMessage: function() {
						var instance = this;

						KeyValueField.superclass._renderErrorMessage.apply(instance, arguments);

						var container = instance.get('container');

						var editorNode = container.one('.key-value-editor');

						editorNode.insert(container.one('.help-block'), 'after');
					},

					_uiSetKey: function(key) {
						var instance = this;

						var keyInput = instance.get('container').one('.key-value-input');

						keyInput.attr('size', instance._getKeyInputSize(key));
						keyInput.val(key);
					},

					_valueGenerationLocked: function() {
						var instance = this;

						return instance.get('key') !== instance.normalizeKey(instance.getContextValue());
					},

					_valueKey: function() {
						var instance = this;

						return instance.normalizeKey(instance.getContextValue());
					}
				}
			}
		);

		Liferay.namespace('DDM.Field').KeyValue = KeyValueField;
	},
	'',
	{
		requires: ['aui-text-unicode', 'event-valuechange', 'liferay-ddm-form-field-text', 'liferay-ddm-form-renderer-field']
	}
);