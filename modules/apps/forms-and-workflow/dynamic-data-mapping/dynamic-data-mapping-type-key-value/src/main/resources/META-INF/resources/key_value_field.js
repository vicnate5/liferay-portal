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
							instance.bindContainerEvent('valuechange', instance._onValueChangeEditorInput, '.key-value-input'),
							instance.bindInputEvent('valuechange', instance._onValueChangeInput)
						);
					},

					getTemplateContext: function() {
						var instance = this;

						return A.merge(
							KeyValueField.superclass.getTemplateContext.apply(instance, arguments),
							{
								key: instance.get('key'),
								strings: instance.get('strings'),
								tooltip: instance.getLocalizedValue(instance.get('tooltip'))
							}
						);
					},

					normalizeKey: function(key) {
						var instance = this;

						key = key.trim();

						for (var i = 0; i < key.length; i++) {
							var item = key[i];

							if (!A.Text.Unicode.test(item, 'L') && !A.Text.Unicode.test(item, 'N')) {
								key = key.replace(item, ' ');
							}
						}

						key = Lang.String.camelize(key, ' ');

						return key.replace(/\s+/ig, '');
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

					saveEditor: function() {
						var instance = this;

						var container = instance.get('container');

						var editorInput = container.one('.key-value-input');

						var value = editorInput.val();

						if (value) {
							instance.set('key', instance.normalizeKey(value));
						}
					},

					_afterKeyChange: function(event) {
						var instance = this;

						var container = instance.get('container');

						var editorInput = container.one('.key-value-input');

						var value = editorInput.val();

						if (value !== event.newVal) {
							instance._uiSetKey(event.newVal);
						}
					},

					_getMaxInputSize: function(str) {
						var size = str.length;

						if (size > 50) {
							size = 50;
						}
						else if (size <= 5) {
							size = 5;
						}

						return size;
					},

					_onValueChangeEditorInput: function(event) {
						var instance = this;

						var value = event.newVal;

						instance.set('key', instance.normalizeKey(value));

						event.target.attr('size', instance._getMaxInputSize(value) + 1);
					},

					_onValueChangeInput: function(event) {
						var instance = this;

						if (!instance.get('generationLocked')) {
							var value = instance.getValue();

							instance.set('key', instance.normalizeKey(value));
						}
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

						var editorInput = instance.get('container').one('.key-value-input');

						editorInput.val(key);
						editorInput.attr('size', instance._getMaxInputSize(key) + 1);
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
		requires: ['aui-text-unicode', 'liferay-ddm-form-field-text', 'liferay-ddm-form-renderer-field']
	}
);