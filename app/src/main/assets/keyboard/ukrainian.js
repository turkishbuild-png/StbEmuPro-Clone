(function(){
    keyboard.addLayout('ukr',
        [
            keyboard.layout('numeric').keys,
            [
                { key: 'й' },
                { key: 'ц' },
                { key: 'у' },
                { key: 'к' },
                { key: 'е' },
                { key: 'н' },
                { key: 'г' },
                { key: 'ш' },
                { key: 'щ' },
                { key: 'з' },
                { key: 'х' },
                { key: 'ї' },
                { items: ['.', ',', '/', '\\'] }
            ],
            [
                { key: 'ф' },
                { key: 'і' },
                { key: 'в' },
                { key: 'а' },
                { key: 'п' },
                { key: 'р' },
                { key: 'о' },
                { key: 'л' },
                { key: 'д' },
                { key: 'ж' },
                { key: 'є' },
                { title: 'Enter', size: 2, key: '\r', shortcut: 'INFO' }
            ],
            [
                { key: 'я' },
                { key: 'ч' },
                { key: 'с' },
                { key: 'м' },
                { key: 'и' },
                { key: 'т' },
                { key: 'ь' },
                { key: 'б' },
                { key: 'ю' },
                { title: '\u21E7', comment: 'Up', key: '\u0026' },
                { items: ['&', '#', '$', '\u2116'] },
                { title: 'Caps Lock', size: 2, onclick: function() { keyboard.onCapsLock() }, shortcut: 'F3' }
            ],
            [
                { items: ['?', '!', '%', '^'] },
                { items: [':', ';', '"' ] },
                { items: ['(', ')', '`', '~'] },
                { title: 'UKR', size: 2, onclick: function() { keyboard.onLangSwitch() }, shortcut: 'F4' },
                { title: 'Space', size: 3, key: ' ', shortcut: 'F1' },
                { title: '\u21E6', comment: 'Left', key: '\u0025' },
                { title: '\u21E9', comment: 'Down', key: '\u0028' },
                { title: '\u21E8', comment: 'Right', key: '\u0027' },
                { title: 'Close', size: 2, onclick: function() { keyboard.close()}, style: 'btn-close'}
            ]
        ]);

})();