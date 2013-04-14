
Resources that need to be changed.

[Build.xml]
* <project name="PluginExample" default="run" basedir=".">
* <property name="jarname" value="exampleplugin_client" />

[calico.plugins.clientExample.iconsets.CalicoIconManager]
* [Line 32]: iconTheme.load( clazz.getResourceAsStream("/calico/iconsets/"+iconThemeName+"/examplepluginicontheme.properties")  );