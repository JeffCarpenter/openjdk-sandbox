tell application "Finder"
  tell disk "DEPLOY_ACTUAL_VOLUME_NAME"
    open
    set current view of container window to icon view
    set toolbar visible of container window to false
    set statusbar visible of container window to false

    set the bounds of container window to {400, 100, 1271, 660}

    set theViewOptions to the icon view options of container window
    set arrangement of theViewOptions to not arranged
    set icon size of theViewOptions to 128
    set background picture of theViewOptions to file "BACKGROUND_IMAGE"
    set position of the item "DEPLOY_APPLICATION_NAME" of container window to {284, 205}
    set position of the item "Applications" of container window to {587, 205}
  end tell
end tell

