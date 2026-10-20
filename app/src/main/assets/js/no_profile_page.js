
function onKeyPress(e)
{
    if(e.keyCode == 13)
    {
        gSTB.EnableServiceButton(true);
        gSTB.StartLocalCfg();
    }
}