import net.unit8.bouncr.BouncrEnkanSystem
import net.unit8.bouncr.component.LdapClient
import static enkan.component.ComponentRelationship.*
enkan.system.EnkanSystem system = new BouncrEnkanSystem().create()
system.setComponent("ldap", new LdapClient())
system.relationships(component("ldap").using("config"))
system.relationships(component("app").using("storeprovider", "datasource", "template", "doma", "jackson", "metrics","realmCache", "config", "jwt", "certificate", "trustManager", "ldap"))
system.start()
