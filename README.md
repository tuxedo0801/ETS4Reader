# ETS4Reader

ETS4 Project Reader ...

Reads .knxproj files and provides  the gathered information in an object hierarchie.

```
File file = new File("myknxproject.knxproj");
KnxProjReader kpr = new KnxProjReader(file);
List<GroupAddress> groupaddressList = kpr.getProjects().get(0).getGroupaddressList();
```
See javadoc of Project, GroupAddress and Device for more details.
