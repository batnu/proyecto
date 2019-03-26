/** 
 * Proyecto: Juego de la vida.
 * Resuelve todos los aspectos del almacenamiento del DTO Usuario 
 * utilizando un ArrayList y un Map - Hashtable.
 * Colabora en el patrón Façade.
 * @since: prototipo2.0
 * @source: UsuariosDAO.java 
 * @version: 2.0 - 2019/03/15 
 * @author: ajp
 */

package accesoDatos.memoria;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import accesoDatos.DAOIndexSort;
import modelo.ClaveAcceso;
import modelo.Correo;
import modelo.DireccionPostal;
import modelo.Identificable;
import modelo.ModeloException;
import modelo.Nif;
import modelo.Usuario;
import modelo.Usuario.RolUsuario;
import util.Fecha;

public class UsuariosDAO extends DAOIndexSort implements OperacionesDAO {

	// Singleton. 
	private static UsuariosDAO instancia = null;

	// Elementos de almacenamiento.
	private List<Identificable> datosUsuarios;
	private Map <String,String> equivalenciasId;

	/**
	 * Constructor por defecto de uso interno.
	 * Sólo se ejecutará una vez.
	 */
	private UsuariosDAO()  {
		datosUsuarios = new ArrayList <Identificable>();
		equivalenciasId = new Hashtable <String, String>();
		cargarPredeterminados();
	}

	/**
	 *  Método estático de acceso a la instancia única.
	 *  Si no existe la crea invocando al constructor interno.
	 *  Utiliza inicialización diferida.
	 *  Sólo se crea una vez; instancia única -patrón singleton-
	 *  @return instancia
	 */
	public static UsuariosDAO getInstancia() {
		if (instancia == null) {
			instancia = new UsuariosDAO();
		}
		return instancia;
	}

	/**
	 *  Método para generar de datos predeterminados.
	 */
	private void cargarPredeterminados() {
		try {
			String nombreUsr = "Admin";
			String password = "Miau#0";	
			alta(new Usuario(new Nif("00000000T"), nombreUsr, "Admin Admin", 
					new DireccionPostal(), 
					new Correo("jv.admin" + "@gmail.com"), 
					new Fecha(0001, 01, 01), 
					new Fecha(), 
					new ClaveAcceso(password), 
					RolUsuario.ADMINISTRADOR));
			
			nombreUsr = "Invitado";
			password = "Miau#0";	
			alta(new Usuario(new Nif("00000001R"), nombreUsr, "Invitado Invitado", 
					new DireccionPostal(), 
					new Correo("jv.invitado" + "@gmail.com"), 
					new Fecha(2000, 01, 01), 
					new Fecha(), 
					new ClaveAcceso(password), 
					RolUsuario.INVITADO));
		} 
		catch (ModeloException | DatosException e) {
			e.printStackTrace();
		}
	}
	
	// OPERACIONES DAO
	
	/**
	 * Búsqueda de usuario dado su id, el correo o su nif.
	 * @param id - el id de Usuario a buscar.
	 * @return - el Usuario encontrado o null si no existe. 
	 */
	@Override
	public Usuario obtener(String id) {
		assert id != null;
		String idUsr = equivalenciasId.get(id);
		int posicion = indexSort(idUsr, datosUsuarios);			// En base 1
		if (posicion > 0) {
			return (Usuario) datosUsuarios.get(posicion - 1);   // En base 0
		}
		return null;				
	}

	/**
	 * obtiene todas los usuarios en una lista.
	 * @return - la lista.
	 */
	@Override
	public List obtenerTodos() {
		return datosUsuarios;
	}

	/**
	 * Registro de nuevo usuario.
	 * @param usr 
	 * @throws DatosException 
	 */
	@Override
	public void alta(Object obj) throws DatosException {
		assert obj != null;
		Usuario usr = (Usuario) obj;									// Para conversión cast.
		int posInsercion = indexSort(usr.getId(), datosUsuarios);

		if (posInsercion < 0) {
			datosUsuarios.add(Math.abs(posInsercion)-1 , usr);  		// Inserta en orden.
			registrarEquivalenciaId(usr);
		}
		else {
			if (!datosUsuarios.get(posInsercion-1).equals(usr)) {
				producirVariantesIdUsr(usr);
			}
			else {
				throw new DatosException("UsuariosDAO.alta:" + usr.getId() +" Ya existe.");
			}
		}
	}
	
	/**
	 *  Si hay coincidencia de identificador hace 23 intentos de variar la última letra
	 *  procedente del NIF. Llama al generarVarianteIdUsr() de la clase Usuario.
	 * @param usrNuevo
	 * @param posicionInsercion
	 * @throws DatosException
	 */
	private void producirVariantesIdUsr(Usuario usr) throws DatosException {
		int posInsercion;
		// Coincidencia de id. Hay que generar variante.
		int intentos = "ABCDEFGHJKLMNPQRSTUVWXYZ".length();
		do {
			// Generar variante y comprueba de nuevo.
			usr = new Usuario(usr);	
			posInsercion = indexSort(usr.getId(), datosUsuarios);
			if (posInsercion < 0) {
				datosUsuarios.add(-posInsercion - 1, usr); // Inserta el usuario en orden.
				registrarEquivalenciaId(usr);
				return;
			}
			intentos--;
		} while (intentos >= 0);
		throw new DatosException("UsuariosDAO.alta: imposible generar variante del " + usr.getId());
	}

	/**
	 *  Añade nif y correo como equivalencias de idUsr para el inicio de sesión. 
	 *	@param usr - Usuario a registrar equivalencias. 
	 */
	private void registrarEquivalenciaId(Usuario usr) {
		assert usr != null;
		equivalenciasId.put(usr.getId(), usr.getId());
		equivalenciasId.put(usr.getNif().getTexto(), usr.getId());
		equivalenciasId.put(usr.getCorreo().getTexto(), usr.getId());
	}

	/**
	 * Elimina el objeto, dado el id utilizado para el almacenamiento.
	 * @param id - el identificador del objeto a eliminar.
	 * @return - el Objeto eliminado. 
	 * @throws DatosException - si no existe.
	 */
	@Override
	public Usuario baja(String id) throws DatosException {
		assert (id != null);
		int posicion = indexSort(id, datosUsuarios); 									// En base 1
		if (posicion > 0) {
			Usuario usrEliminado = (Usuario) datosUsuarios.remove(posicion-1); 	// En base 0
			equivalenciasId.remove(usrEliminado.getId());
			equivalenciasId.remove(usrEliminado.getNif().getTexto());
			equivalenciasId.remove(usrEliminado.getCorreo().getTexto());
			return usrEliminado;
		}
		else {
			throw new DatosException("UsuariosDAO.baja: "+ id + " no existe");
		}
	} 
	
	/**
	 *  Actualiza datos de un Usuario reemplazando el almacenado por el recibido. 
	 *  No admitirá cambios en el idUsr.
	 *	@param obj - Usuario con los cambios.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public void actualizar(Object obj) throws DatosException  {
		assert obj != null;
		Usuario usrActualizado = (Usuario) obj;									// Para conversión cast.
		int posicion = indexSort(usrActualizado.getId(), datosUsuarios); 		// En base 1
		if (posicion > 0) {
			// Reemplaza equivalencias de Nif y Correo.
			Usuario usrModificado = (Usuario) datosUsuarios.get(posicion-1); 	// En base 0
			equivalenciasId.remove(usrModificado.getNif().getTexto());
			equivalenciasId.remove(usrModificado.getCorreo().getTexto());
			equivalenciasId.put(usrActualizado.getNif().getTexto(), usrActualizado.getId());
			equivalenciasId.put(usrActualizado.getCorreo().getTexto(), usrActualizado.getId());	
			// Reemplaza elemento
			datosUsuarios.set(posicion-1, usrActualizado);  					// En base 0		
		}
		else {
			throw new DatosException("UsuariosDAO.actualizar: "+ usrActualizado.getId() + " no existe");
		}
	} 

	/**
	 * Obtiene el listado de todos los usuarios almacenados.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarDatos() {
		StringBuilder result = new StringBuilder();
		for (Identificable usr: datosUsuarios) {
			result.append("\n" + usr); 
		}
		return result.toString();
	}

	/**
	 * Obtiene el listado de todos id de los objetos almacenados.
	 * @return el texto con el volcado de id.
	 */
	@Override
	public String listarId() {
		StringBuilder result = new StringBuilder();
		for (Identificable usr: datosUsuarios) {
			result.append("\n" + usr.getId()); 
		}
		return result.toString();
	}
	
	/**
	 * Elimina todos los usuarios almacenados y regenera los predeterminados.
	 */
	@Override
	public void borrarTodo() {
		datosUsuarios.clear();
		equivalenciasId.clear();
		cargarPredeterminados();
	}

} //class