# FloatDash v0.2

Crea **combos** de apps (1 a 4) y los abre en **ventanas flotantes (freeform)**
sobre Nova, cada app en la región que vos definís. Pensada para la pantalla TS18
de la L200, pero funciona en cualquier Android con freeform activo.

## Requisitos
En *Opciones de desarrollador* (ya activados en tu unidad):
- Enable freeform windows
- Force activities to be resizable

Y dentro de la app: permiso **"Mostrar sobre otras apps"** (la app te lleva a
concederlo). Es lo que permite abrir **varias** apps seguidas.

## Flujo
1. **Nuevo combo** → ponle nombre → elegí un preset (1 / 2V / 2H / 3 / 4).
2. En el lienzo: tocá cada región para asignarle una app; arrastrá para mover,
   esquina para redimensionar.
3. **Guardar**.
4. En la lista, el botón ⌂ ("A inicio") crea un ícono en Nova que lanza todo el
   combo en su sitio. El ícono dibuja los logos reales de las apps en su región.
5. También quedan accesos rápidos **Waze Flotante** / **Maps Flotante** (una app).

## Firma
Mismo keystore que L200 Dash/Cam (alias `l200dash`). Secrets del repo:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
No usa Maps API key.

## Compilar
Push a `main` → GitHub Actions genera `FloatDash.apk` en Releases/Artifacts.

## Notas de diseño / límites
- Máximo 4 apps por combo (más freeform se vuelve inestable en TS18).
- Si una ventana arranca maximizada o rara, es el comportamiento freeform de la
  ROM; ajustar regiones suele ayudar.
- Para publicar en Play Store haría falta subir targetSdk, manejar dispositivos
  sin freeform, política de privacidad y desbloqueo por compra (fase posterior).
