if(typeof netscape == 'undefined')
{
    netscape = {
        security: {
            PrivilegeManager: {
                enablePrivilege: function(name){
                    console.log('STUB: netscape.security.PrivilegeManager.enablePrivilege("'+name+'")')
                }
            }
        }
    };
    console.log('[BUG]: netscape BUG!')
}