package chibson.usuarios.config;

import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Permiso;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.PermisoRepository;
import chibson.usuarios.repository.RolRepository;
import chibson.usuarios.repository.UsuarioRepository;
import chibson.usuarios.service.Permisos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Crea al arrancar, solo si no existen: los permisos, los roles ADMINISTRADOR y CLIENTE,
 * y un administrador inicial para poder entrar al panel la primera vez.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    public static final String RUT_ADMIN = "12345678-5";
    public static final String CORREO_ADMIN = "admin@chibson.cl";
    public static final String PASSWORD_ADMIN = "Admin1234";

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public DatosIniciales(PermisoRepository permisoRepository, RolRepository rolRepository,
                          UsuarioRepository usuarioRepository) {
        this.permisoRepository = permisoRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        Map<String, String> permisos = new LinkedHashMap<>();
        permisos.put(Permisos.GESTIONAR_PRODUCTOS, "Gestionar catálogo e inventario");
        permisos.put(Permisos.GESTIONAR_PEDIDOS, "Gestionar pedidos y devoluciones");
        permisos.put(Permisos.GESTIONAR_USUARIOS, "Gestionar usuarios, roles y permisos");
        permisos.put(Permisos.VER_VENTAS, "Ver registro de ventas");
        permisos.put(Permisos.GESTIONAR_RESENAS, "Moderar y responder reseñas");
        permisos.put(Permisos.GESTIONAR_SOPORTE, "Atender tickets de soporte");
        permisos.put(Permisos.GESTIONAR_NOTICIAS, "Gestionar noticias e información institucional");
        permisos.put(Permisos.VER_INFORMES, "Ver dashboard e informes");
        permisos.put(Permisos.VER_HISTORIAL, "Ver historial de acciones");
        permisos.forEach((codigo, descripcion) -> {
            if (permisoRepository.findByCodigo(codigo).isEmpty()) {
                permisoRepository.save(new Permiso(null, codigo, descripcion));
            }
        });

        Rol admin = rolRepository.findByNombreIgnoreCase(Rol.ADMINISTRADOR).orElseGet(() -> {
            Rol rol = new Rol(null, Rol.ADMINISTRADOR, new HashSet<>(permisoRepository.findAll()));
            return rolRepository.save(rol);
        });
        if (rolRepository.findByNombreIgnoreCase(Rol.CLIENTE).isEmpty()) {
            rolRepository.save(new Rol(null, Rol.CLIENTE, new HashSet<>()));
        }

        if (!usuarioRepository.existsById(RUT_ADMIN)) {
            Usuario usuario = new Usuario(RUT_ADMIN, "Administrador", "Chibson", "Instruments", "admin",
                    CORREO_ADMIN, "+56912345678", PASSWORD_ADMIN, null, null, null, null,
                    EstadoUsuario.ACTIVO, LocalDate.now(), admin);
            usuarioRepository.save(usuario);
            log.info("Administrador inicial creado: {} / {}", CORREO_ADMIN, PASSWORD_ADMIN);
        }
    }
}
