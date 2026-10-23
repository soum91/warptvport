"""Apply a private TV adaptation to an apktool-decoded 6.38.11 base.

This does not supply Cloudflare's native libraries or produce a runnable APK.
The matching armeabi-v7a split must be supplied before packaging for Sony VH22.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parent / 'decoded'
A = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', A[1:-1])

p = ROOT / 'AndroidManifest.xml'
tree = ET.parse(p)
manifest = tree.getroot()
if manifest.get('package') != 'com.cloudflare.onedotonedotonedotone':
    raise SystemExit('Use a fresh decoded official base, before applying this patch.')
app = manifest.find('application')
app.set(A+'extractNativeLibs', 'true')
app.set(A+'banner', '@drawable/tv_banner')
for name in ('android.software.leanback', 'android.hardware.touchscreen'):
    item = ET.SubElement(manifest, 'uses-feature')
    item.set(A+'name', name)
    item.set(A+'required', 'false')
for activity in app.findall('activity'):
    if activity.get(A+'name', '').startswith('com.cloudflare.app.'):
        activity.set(A+'screenOrientation', 'landscape')
        activity.set(A+'banner', '@drawable/tv_banner')
    for intent in activity.findall('intent-filter'):
        if any(c.get(A+'name') == 'android.intent.category.LAUNCHER' for c in intent.findall('category')):
            c = ET.SubElement(intent, 'category')
            c.set(A+'name', 'android.intent.category.LEANBACK_LAUNCHER')
tree.write(p, encoding='utf-8', xml_declaration=True)

(ROOT/'res/drawable/tv_focus.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
 <item android:state_focused="true"><shape><solid android:color="#22F48120"/><stroke android:width="3dp" android:color="#F48120"/><corners android:radius="14dp"/></shape></item>
 <item><shape><solid android:color="#00000000"/><corners android:radius="14dp"/></shape></item>
</selector>''')

# Keep every ID and widget type expected by the original view binding.
control = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto"
 android:id="@id/container" android:orientation="horizontal" android:gravity="center" android:clipChildren="false"
 android:clipToPadding="false" android:layout_width="match_parent" android:layout_height="match_parent"
 android:paddingStart="48dp" android:paddingEnd="48dp" android:paddingTop="16dp" android:paddingBottom="16dp"
 android:filterTouchesWhenObscured="true">
 <LinearLayout android:orientation="vertical" android:gravity="center_vertical" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:paddingEnd="40dp">
  <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="PRIVATE CONNECTION" android:textSize="14sp" android:letterSpacing="0.15" android:textColor="#F48120"/>
  <com.cloudflare.app.presentation.widget.logoview.LogoView android:id="@id/logoView" android:layout_width="match_parent" android:layout_height="76dp" android:layout_marginTop="20dp"/>
  <TextView android:id="@id/unlimitedLabel" android:visibility="gone" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/unlimited" android:textSize="20sp" android:textColor="@color/text_regular"/>
  <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="A more private connection for your TV." android:textSize="22sp" android:textColor="@color/text_regular"/>
  <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:text="Use the remote to select the switch. Press OK to connect or disconnect. Open Settings for WARP and DNS options." android:textSize="16sp" android:textColor="@color/text_regular" android:alpha="0.7"/>
  <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="22dp" android:text="Unofficial TV adaptation of Cloudflare 1.1.1.1" android:textSize="12sp" android:textColor="@color/text_regular" android:alpha="0.55"/>
 </LinearLayout>
 <LinearLayout android:orientation="vertical" android:gravity="center" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:padding="16dp">
  <com.cloudflare.app.presentation.widget.CometSwitch android:id="@id/launchSwitch" android:layout_width="wrap_content" android:layout_height="wrap_content" android:padding="16dp" android:focusable="true" android:focusableInTouchMode="false" android:foreground="@drawable/tv_focus" android:nextFocusUp="@id/settingsBtn" android:nextFocusLeft="@id/settingsBtn" android:nextFocusRight="@id/settingsBtn" android:contentDescription="Connect or disconnect" app:showText="false" app:track="@drawable/main_switch_track_default"><requestFocus/></com.cloudflare.app.presentation.widget.CometSwitch>
  <TextView android:id="@id/connectionStateTv" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:layout_marginTop="20dp" android:textSize="26sp" android:textColor="@color/text_regular"/>
  <TextView android:id="@id/connectionDescTv" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:layout_marginTop="12dp" android:textSize="17sp" android:textColor="@color/text_regular"/>
  <FrameLayout android:id="@id/pqButtonFrameLayout" android:visibility="gone" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="16dp">
   <Button android:id="@id/pqButton" android:layout_width="wrap_content" android:layout_height="48dp" android:minWidth="200dp" android:text="@string/pq_button" android:textAllCaps="false" android:textSize="14sp" android:focusable="true" android:background="@drawable/button_rounded_outline_orange" android:textColor="@color/accent_warp"/>
  </FrameLayout>
 </LinearLayout>
</LinearLayout>'''
for directory in ROOT.glob('res/layout*'):
    path = directory/'fragment_main_control.xml'
    if path.exists(): path.write_text(control)

# Landscape dashboard reserves the upper row for remote-accessible toolbar controls.
main = (ROOT/'res/layout/activity_main.xml').read_text()
main = main.replace('android:layout_height="32.0dip"', 'android:layout_height="24.0dip"')
for view_id in ('guideBtn', 'settingsBtn', 'submitFeedbackBtn'):
    token = 'android:id="@id/'+view_id+'"'
    main = main.replace(token, token+' android:focusable="true" android:clickable="true" android:foreground="@drawable/tv_focus" android:nextFocusDown="@id/launchSwitch"')
main = main.replace('android:paddingLeft="16.0dip"', 'android:paddingLeft="40.0dip"').replace('android:paddingRight="16.0dip"', 'android:paddingRight="40.0dip"')
(ROOT/'res/layout/activity_main.xml').write_text(main)
land = ROOT/'res/layout-land'
land.mkdir(exist_ok=True)
(land/'activity_main.xml').write_text(main)

# Give custom clickable settings rows, which are not standard buttons, a focus target.
for path in ROOT.glob('res/layout*/*.xml'):
    content = path.read_text()
    import re
    content = re.sub(r'<com\.cloudflare\.app\.presentation\.widget\.SettingsRow\b([^>]*?)(/?)>',
        lambda m: '<com.cloudflare.app.presentation.widget.SettingsRow'+m[1]+('' if 'android:focusable=' in m[1] else ' android:focusable="true" android:foreground="@drawable/tv_focus"')+m[2]+'>', content)
    path.write_text(content)

# Runtime adapter also handles controls made clickable after XML inflation.
compat = ROOT/'smali/androidx/appcompat/app/AppCompatActivity.smali'
content = compat.read_text()
start = content.index('.method public onPostResume()V')
end = content.index('.end method', start)
method = content[start:end]
method = method.replace('    return-void', '    invoke-static {p0}, Ltv/RemoteSupport;->install(Landroid/app/Activity;)V\n\n    return-void')
content = content[:start]+method+content[end:]
start = content.index('.method public dispatchKeyEvent(Landroid/view/KeyEvent;)Z')
local = content.index('    .locals 3', start)+len('    .locals 3')
content = content[:local]+'''
    invoke-static {p0, p1}, Ltv/RemoteSupport;->activate(Landroid/app/Activity;Landroid/view/KeyEvent;)Z
    move-result v0
    if-eqz v0, :tv_original_key
    const/4 v0, 0x1
    return v0
    :tv_original_key
'''+content[local:]
compat.write_text(content)
helper = ROOT/'smali/tv'
helper.mkdir(exist_ok=True)
(helper/'RemoteSupport.smali').write_text((Path(__file__).parent/'RemoteSupport.smali').read_text())
import shutil
banner = ROOT/'res/drawable-nodpi/tv_banner.png'
banner.parent.mkdir(exist_ok=True)
shutil.copyfile(Path(__file__).parent/'tv_banner.png', banner)

# Keep the original Java/Kotlin namespace, but give the installed app its own ID.
old_id = 'com.cloudflare.onedotonedotonedotone'
new_id = 'com.soum91.warptv'
for path in ROOT.rglob('*'):
    relative_parts = path.relative_to(ROOT).parts
    if path.is_file() and path.suffix in ('.smali', '.xml') and 'original' not in relative_parts and '.rsync-tmp' not in relative_parts:
        try:
            content = path.read_text()
        except (UnicodeDecodeError, FileNotFoundError):
            continue
        if old_id in content:
            path.write_text(content.replace(old_id, new_id))
tree = ET.parse(ROOT/'AndroidManifest.xml')
app = tree.getroot().find('application')
app.set(A+'label', 'WARP TV')
for meta in list(app.findall('meta-data')):
    if meta.get(A+'name', '').startswith('com.android.stamp.'):
        app.remove(meta)
tree.write(ROOT/'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
config = ROOT/'apktool.yml'
content = config.read_text().replace('versionCode: 5703', 'versionCode: 5703003').replace('versionName: 6.38.11', 'versionName: 6.38.11-tv.3').replace('minSdkVersion: 24', 'minSdkVersion: 26')
config.write_text(content)

# After Android 12 accepts Cloudflare's VPN profile flow, the stock result
# callback only starts the service when an unrelated deferred result field is
# set. On a fresh TV install that field is null, leaving the switch enabled but
# the tunnel stopped. If this activity returned with VPN permission granted,
# start the normal Cloudflare flow from the existing ViewModel.
profile_result = ROOT/'smali/com/cloudflare/app/presentation/main/maincontrol/e.smali'
content = profile_result.read_text()
anchor = '    iget-object v0, p1, Lcom/cloudflare/app/presentation/main/maincontrol/MainControlFragment;->r:Ljava/lang/Integer;'
if content.count(anchor) != 1:
    raise SystemExit('Could not locate the VPN activity-result callback anchor.')
content = content.replace(anchor, '    const/4 v5, 0x0\n'+anchor, 1)
anchor = '    if-eqz v0, :cond_1'
if content.count(anchor) != 1:
    raise SystemExit('Could not locate deferred-result branch.')
content = content.replace(anchor, anchor+'\n    const/4 v5, 0x1', 1)
anchor = '    :cond_1\n    return-void'
if content.count(anchor) != 1:
    raise SystemExit('Could not locate callback exit for activation restart.')
new = '''    :cond_1
    if-nez v5, :tv_profile_done
    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v2, 0x21
    if-lt v1, v2, :tv_check_profile_permission
    goto :tv_profile_done
    :tv_check_profile_permission
    invoke-virtual {p1}, Lcom/cloudflare/app/presentation/main/maincontrol/MainControlFragment;->k()Lcom/cloudflare/app/presentation/main/maincontrol/MainControlViewModel;
    move-result-object v0
    iget-object v1, v0, Lcom/cloudflare/app/presentation/main/maincontrol/MainControlViewModel;->a:Lcom/cloudflare/app/domain/connectiontest/VpnProfileStatusService;
    invoke-virtual {v1}, Lcom/cloudflare/app/domain/connectiontest/VpnProfileStatusService;->b()Z
    move-result v1
    if-eqz v1, :tv_profile_done
    invoke-virtual {v0}, Lcom/cloudflare/app/presentation/main/maincontrol/MainControlViewModel;->d()V
    :tv_profile_done
    return-void'''
content = content.replace(anchor, new, 1)
profile_result.write_text(content)

# A fresh TV install can reach the dashboard before Cloudflare has a device
# registration ID. The stock toggle then resets itself and shows a managed-
# device authentication toast, so it never reaches Android's VPN consent flow.
# Send this state back through Cloudflare's own terms/registration activity.
registration = ROOT/'smali/com/cloudflare/app/presentation/main/maincontrol/MainControlFragment$onViewCreated$2.smali'
content = registration.read_text()
guard = '    if-eqz v2, :cond_3\n'
start = content.index(guard) + len(guard)
end = content.index('    :cond_3\n', start)
content = content[:start] + '''
    iget-object p1, v1, Lcom/cloudflare/app/presentation/main/maincontrol/MainControlFragment;->a:Lcom/cloudflare/app/databinding/FragmentMainControlBinding;
    invoke-static {p1}, Lkotlin/jvm/internal/Intrinsics;->c(Ljava/lang/Object;)V
    iget-object p1, p1, Lcom/cloudflare/app/databinding/FragmentMainControlBinding;->d:Lcom/cloudflare/app/presentation/widget/CometSwitch;
    invoke-virtual {p1, v0}, Lcom/cloudflare/app/presentation/widget/CometSwitch;->setChecked(Z)V
    new-instance v5, Landroid/content/Intent;
    invoke-virtual {v1}, Landroidx/fragment/app/Fragment;->requireActivity()Landroidx/fragment/app/FragmentActivity;
    move-result-object v6
    const-class v7, Lcom/cloudflare/app/presentation/onboarding/termsacceptance/TermsAcceptanceActivity;
    invoke-direct {v5, v6, v7}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    invoke-virtual {v6, v5}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
    goto/16 :goto_4

''' + content[end:]
registration.write_text(content)
print('TV manifest, dashboard, focus controls, remote adapter and registration recovery prepared.')
