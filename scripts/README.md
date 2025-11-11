# Scripts

Este directorio contiene scripts útiles para el desarrollo y mantenimiento del proyecto.

## 🔒 validate-security.sh

Script de validación de seguridad que verifica que no haya credenciales expuestas en el código.

### Uso:

```bash
./scripts/validate-security.sh
```

### Qué verifica:

1. ✅ No hay contraseñas hardcodeadas
2. ✅ No hay API keys o tokens expuestos
3. ✅ El archivo `.env` está en `.gitignore`
4. ✅ Existe el archivo `.env.example`
5. ✅ El archivo `.env` no está trackeado por git
6. ✅ No hay patrones de credenciales conocidas
7. ✅ `docker-compose.yaml` usa variables de entorno

### Integración con Git

Se recomienda ejecutar este script antes de hacer commit. Puedes configurar un pre-commit hook:

```bash
# Agregar al archivo .git/hooks/pre-commit
#!/bin/bash
./scripts/validate-security.sh
```

O hacerlo manualmente:

```bash
./scripts/validate-security.sh && git commit -m "Your message"
```

## Otros Scripts

(Agregar documentación de otros scripts según se vayan creando)
