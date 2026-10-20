(function(){
    keyboard.addLayout('eng',
    [
        keyboard.layout('numeric').keys,
        [
            { key: '/' },
            { key: 'q' },
            { key: 'w' },
            { key: 'e' },
            { key: 'r' },
            { key: 't' },
            { key: 'y' },
            { key: 'u' },
            { key: 'i' },
            { key: 'o' },
            { key: 'p' },
            { items: ['.', ','] },
            { items: ['[', ']'] }
        ],
        [
            { key: '@' },
            { key: 'a' },
            { key: 's' },
            { key: 'd' },
            { key: 'f' },
            { key: 'g' },
            { key: 'h' },
            { key: 'j' },
            { key: 'k' },
            { key: 'l' },
            { items: ['\\', '*', '<', '>'] },
            { title: 'Enter', size: 2, key: '\r', shortcut: 'INFO' }
        ],
        [
            { items: ['?', '!', '%', '^'] },
            { items: [':', ';', '"', '\''] },
            { key: 'z' },
            { key: 'x' },
            { key: 'c' },
            { key: 'v' },
            { key: 'b' },
            { key: 'n' },
            { key: 'm' },
            { title: '\u21E7', comment: 'Up', key: '\u0026' },
            { items: ['&', '#', '$', '\u2116'] },
            { title: 'Caps Lock', size: 2, onclick: function() { keyboard.onCapsLock() }, shortcut: 'F3' }
        ],
        [
            { items: ['`', '~'] },
            { items: ['(', ')', '[', ']' ] },
            { title: '.com', style: 'less-font-size' },
            { title: 'ENG', size: 2, onclick: function() { keyboard.onLangSwitch() }, shortcut: 'F4' },
            { title: 'Space', size: 3, key: ' ', shortcut: 'F1' },
            { title: '\u21E6', comment: 'Left', key: '\u0025' },
            { title: '\u21E9', comment: 'Down', key: '\u0028' },
            { title: '\u21E8', comment: 'Right', key: '\u0027' },
            { title: 'Close', size: 2, onclick: function() { keyboard.close()}, style: 'btn-close' }
        ]
    ]);

})();