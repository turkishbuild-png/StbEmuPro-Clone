var keyboard = new Keyboard();

function init()
{
    keyboard.init();
}

function Keyboard()
{
    this.is_caps_lock = false;
    this._layouts = {};
    this.text_buffer = '';
    this._languages = [];
}

/**
 *
 * @param {KeyboardConfig} config
 */
Keyboard.prototype.filterLanguages = function(config)
{
    console.log('KEYBOARD CONFIG: ' + JSON.stringify(config));

    var filter = (function(languages) {
        return function(item)
        {
            return languages.indexOf(item) !== -1;
        }
    })(this._languages);

    this._languages = config.languages
        .filter(filter);

    this._language = this._languages.indexOf(config.active_language) !== -1 ? config.active_language : "eng";
    console.log('Language list: ' + JSON.stringify(this._languages));
    console.log('Active language: ' + this._language);
};

Keyboard.prototype.init = function()
{
    var self = this;

    var config = JSON.parse(api.loadConfig());
    this.filterLanguages(config);

    if(config.show_selector)
        document.styleSheets[0].insertRule('.keyboard-key:focus {color: blue; background-color: yellow; }', 0);

    this.redrawKeyboard();
    this.switch_preview_btn = document.getElementById('preview_window_btn');
    this.close_btn = document.getElementById('close_btn');
    this.preview_window = document.getElementById('preview_window');
    this.preview_window.innerHTML = '';

    this.key_menu = document.getElementById('key_menu');
    this.menu_key_ok = document.getElementById('key_menu_item_1_1');
    this.menu_key_ok.className = 'key-menu-item ok';
    this.menu_key_ok.href = 'javascript:void(0)';
    this.menu_key_ok.onclick = function() { self.hideKeyMenu(); };

    this.overlay_bg = document.getElementById('overlay_bg');
    this.overlay_bg.onclick = function() { self.hideKeyMenu(); };
    this.button_size = document.getElementById('key_td_0_0').offsetWidth;

    document.getElementById('key_0_0').focus();
};

/**
 *
 * @param {string} name
 * @param {Object[]} keys
 * @param {boolean?} can_switch
 */
Keyboard.prototype.addLayout = function(name, keys, can_switch)
{
    can_switch = can_switch == undefined || can_switch;
    this._layouts[name] = {
        can_switch: can_switch,
        keys: keys
    };
    if(can_switch)
        this._languages.push(name);
};

/**
 *
 * @param name
 * @returns {Object[]}
 */
Keyboard.prototype.layout = function(name)
{
    return this._layouts[name];
};

Keyboard.prototype.updateTextBuffer = function()
{
    this.preview_window.innerHTML = this.text_buffer;
};

/**
 *
 * @param {string} char
 */
Keyboard.prototype.addCharacter = function(char)
{
    //this.text_buffer += char;
    //this.updateTextBuffer();
    console.log('Keyboard::addCharacter(' + (char.charCodeAt(0)) + ')');
    api.onKeyCode(char.charCodeAt(0));
};

Keyboard.prototype.onCapsLock = function()
{
    this.is_caps_lock = !this.is_caps_lock;
    this.redrawKeyboard();
};

Keyboard.prototype.onBackspace = function()
{
    if(this.text_buffer.length > 0)
        this.text_buffer = this.text_buffer.substr(0, this.text_buffer.length - 1);
    this.updateTextBuffer();
};

Keyboard.prototype.changeKeyState = function(enabled)
{
    var keys = document.getElementsByClassName('keyboard-key');
    console.log('keys', keys.length);
    for(var index=0; index < keys.length; index++)
    {
        if(enabled)
        {
            keys[index].removeAttribute('disabled');
            keys[index].removeAttribute("tabIndex");
        }

        else {
            keys[index].setAttribute("tabIndex", "-1");
            keys[index].setAttribute('disabled', 'disabled');
        }
    }
};

Keyboard.prototype.onLangSwitch = function()
{
    var index = this._languages.indexOf(this._language);
    if(index == -1)
        index = 0;
    else {
        index++;
        if(index >= this._languages.length)
            index = 0;
    }
    this._language = this._languages[index];
    console.log('language', this._language);
    api.onLanguageChanged(this._language);
    this.redrawKeyboard();
};

Keyboard.prototype.updateKeyMenu = function(items, pos)
{
    var elements = [];
    elements[0] = document.getElementById('key_menu_item_0_1');
    elements[1] = document.getElementById('key_menu_item_1_0');
    elements[2] = document.getElementById('key_menu_item_1_2');
    elements[3] = document.getElementById('key_menu_item_2_1');

    for(var i = 0; i < elements.length; i++)
    {
        elements[i].innerHTML = '';
        elements[i].className = 'key-menu-item';
        elements[i].onclick = null;
        elements[i].removeAttribute('href');
    }

    for(var index = 0; index < items.length; index++)
    {
        elements[index].innerHTML = items[index];
        elements[index].$character = items[index];
        elements[index].className = 'key-menu-item button';
        elements[index].href = 'javascript:void(0)';
        elements[index].onclick = onClickDefault;
    }

    console.log('pos: ' + JSON.stringify(pos));

    console.log('size:' +  this.button_size, document.body.clientHeight, document.body.clientWidth);
    var top = pos.top - this.button_size + 3;
    if(top < 0)
        top = 0;
    if(top + this.button_size*3 > document.body.clientHeight)
        top = document.body.clientHeight - this.button_size*3;

    var left = pos.left - this.button_size + 3;
    if(left < 0)
        left = 0;
    if(left + this.button_size*3 > document.body.clientWidth)
        left = document.body.clientWidth - this.button_size*3;


    this.key_menu.style.left = left + 'px';
    this.key_menu.style.top = top + 'px';
};

Keyboard.prototype.showKeyMenu = function()
{
    var self = this;
    setTimeout(function(){
        self.changeKeyState(false);
        self.overlay_bg.style.display = 'block';
        self.key_menu.style.display = 'block';
        self.menu_key_ok.focus();
    }, 50);
};

Keyboard.prototype.hideKeyMenu = function()
{
    this.changeKeyState(true);
    this.key_menu.style.display = 'none';
    this.overlay_bg.style.display = 'none';
};

Keyboard.prototype.close = function()
{
    console.log('close()');
    api.close();
};

var onClickDefault = function()
{
    keyboard.hideKeyMenu();
    keyboard.addCharacter(keyboard.is_caps_lock ? this.$character.toUpperCase() : this.$character);
};

Keyboard.prototype.redrawKeyboard = function()
{
    var native_lang = this._layouts[this._language].keys;
    for(var row = 0; row < 5; row++)
    {
        var curr_row = native_lang[row];
        for(var col = 0; col < curr_row.length; col++)
        {
            var item = curr_row[col];
            var td = document.getElementById('key_td_' + row + '_' + col);
            var key_elem = document.getElementById('key_' + row + '_' + col);
            key_elem.onclick = null;

            var title = '-';
            if(item.title != undefined)
            {
                title = item.title;
                if(item.shortcut != undefined)
                {
                    var two_rows = title.indexOf(' ') != -1 ? 'two-rows' : '';
                    title = '<span class="control-big"><span>[' + item.shortcut + ']</span><span class="' + two_rows +'">'
                        + title + '</span></span>';

                    key_elem.$shortcut = item.shortcut;
                    key_elem.$two_rows = two_rows;
                    key_elem.$title = title;
                }

            }
            else if(item.items != undefined)
            {
                title = '<div class="item-menu-control size-' + item.items.length + '">';
                for(var index = 0; index < item.items.length; index++)
                {
                    title += '<div>' + item.items[index] + '</div>';
                }
                title += '</div>';
                key_elem.$items = item.items;
                key_elem.$pos = getOffset(key_elem);
                key_elem.onclick = function() {
                    keyboard.updateKeyMenu(this.$items, this.$pos);
                    keyboard.showKeyMenu();
                }
            }
            else
            {

                title = item.key + '';
                if(this.is_caps_lock)
                    title = title.toUpperCase();
            }

            var size = item.size || 1;

            if(item.key != undefined)
                key_elem.$character = item.key;

            key_elem.innerHTML = title;
            key_elem.className = 'keyboard-key size-' + size;

            if(item.style)
                key_elem.className += ' ' + item.style;

            td.setAttribute('colspan', size);

            if(key_elem.onclick === null)
            {
                if(item.onclick != undefined)
                {
                    key_elem.onclick = item.onclick;
                }
                else
                {
                    key_elem.onclick = onClickDefault;
                }
            }
        }
    }
};

function getOffset( el ) {
    var _x = 0;
    var _y = 0;
    while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
        _x += el.offsetLeft - el.scrollLeft;
        _y += el.offsetTop - el.scrollTop;
        el = el.offsetParent;
    }
    return { top: _y, left: _x };
}
