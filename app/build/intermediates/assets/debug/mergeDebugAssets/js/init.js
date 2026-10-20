function init()
{

    loadResource('content://com.mvas.stbemu.string/init_page_text', 'inner_div');

}

function loadResource(resourceId, containerId)
{
    var request = new XMLHttpRequest();
    var container = document.getElementById(containerId);
    request.open('GET', resourceId, true);
    request.onreadystatechange = function() {
        if (request.readyState === 4){
            container.innerHTML = request.responseText;
        }
    };
    request.send(null);
}