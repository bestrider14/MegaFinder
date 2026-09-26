use std::process::Command;

#[tauri::command]
fn open_file(path: String) -> Result<(), String> {
  if path.trim().is_empty() {
    return Err("Le chemin du fichier est vide".to_string());
  }

  #[cfg(target_os = "windows")]
  let result = Command::new("explorer.exe").arg(&path).spawn();

  #[cfg(target_os = "macos")]
  let result = Command::new("open").arg(&path).spawn();

  #[cfg(all(unix, not(target_os = "macos")))]
  let result = Command::new("xdg-open").arg(&path).spawn();

  result
    .map(|_| ())
    .map_err(|error| format!("Impossible de lancer l’application associée : {error}"))
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .invoke_handler(tauri::generate_handler![open_file])
    .setup(|app| {
      if cfg!(debug_assertions) {
        app.handle().plugin(
          tauri_plugin_log::Builder::default()
            .level(log::LevelFilter::Info)
            .build(),
        )?;
      }
      Ok(())
    })
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
