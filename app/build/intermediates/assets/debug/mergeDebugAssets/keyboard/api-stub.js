function KeyboardConfig()
{
    this.languages = ["eng", "rus", "ukr"];
    this.active_language = "eng";
    this.show_selector = false;
}


function Api() {
    console.log('Using stub API methods!');
}

/**
 * Close keyboard
 */
Api.prototype.close = function(){};
/**
 *
 * @param {int} keyCode
 */
Api.prototype.onKeyCode = function(keyCode){};
/**
 *
 * @param {string} str
 */
Api.prototype.onString = function(str) {};

/**
 *
 * @param {string} language
 */
Api.prototype.onLanguageChanged = function(language){};

/**
 * @return {string} JSON data
 */
Api.prototype.loadConfig = function(){
    return JSON.stringify(new KeyboardConfig());
};


var api = new Api();