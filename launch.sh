 
#!/bin/bash
pars="";
for arg; do pars=$pars" "$arg; done;
java -jar DipolePlayer.jar $pars
