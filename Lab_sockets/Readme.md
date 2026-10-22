Estructura del TCP

Utilice la misma estructura que mostro el profesor en los codigos de ejemplos, cambiando unicamente la logica de ECO por status, Calibrate y error  



Comportamiento cuando se cierra abruptamente el servidor 
    En TCP cuando forzamos el cierre del servidor TCP y mantenemos al cliente activo y le hacemos una peticion al servidor nos saltara la exepcion y nos mostrara el mensaje que imprimimos en la linea 40 del clienteTCP y se cerrara el cliente 