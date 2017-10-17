package backendexample;

import backendexample.ldap.InMemoryDirectoryServiceFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.IOUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

import java.util.Arrays;

public class LdapStandaloneServer {
    private static final String LDIF_FILENAME_JBOSS_ORG = "jboss-org.ldif";

    private final DirectoryService directoryService;
    private final org.apache.directory.server.ldap.LdapServer ldapServer;

    // Public methods --------------------------------------------------------

    /**
     * Starts an LDAP server.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new LdapStandaloneServer();
    }

    /**
     * Create a single LDAP server.
     *
     * @throws Exception
     */
    public LdapStandaloneServer() throws Exception {
        long startTime = System.currentTimeMillis();

        InMemoryDirectoryServiceFactory dsFactory = new InMemoryDirectoryServiceFactory();
        dsFactory.init("ds");

        directoryService = dsFactory.getDirectoryService();
        System.out.println("Directory service started in " + (System.currentTimeMillis() - startTime) + "ms");
        directoryService.setAllowAnonymousAccess(true);
        importLdif("src/dev/resources/ldap/microsoft.ldif",
                "src/dev/resources/ldap/users.ldif");

        ldapServer = new org.apache.directory.server.ldap.LdapServer();
        TcpTransport tcp = new TcpTransport("0.0.0.0", 10389);
        TcpTransport ldapsTcp = new TcpTransport("0.0.0.0", 10636);
        ldapsTcp.setEnableSSL(true);
        ldapsTcp.setEnabledProtocols(Arrays.asList("TLSv1.2"));
        ldapServer.setKeystoreFile("src/dev/resources/bouncr.jks");
        ldapServer.setCertificatePassword("password");
        ldapServer.setTransports(tcp, ldapsTcp);
        ldapServer.setDirectoryService(directoryService);
        ldapServer.start();

        System.out.println("You can connect to the server now");
        final String host = "127.0.0.1";
        System.out.println("URL:      ldap://" + formatPossibleIpv6(host) + ":" + 10389);
        System.out.println("User DN:  uid=admin,ou=system");
        System.out.println("Password: secret");
        System.out.println("LDAP server started in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Stops LDAP server and the underlying directory service.
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
    }

    /**
     * Imports given LDIF file to the directory using given directory service and schema manager.
     *
     * @param ldifFiles
     * @throws Exception
     */
    private void importLdif(String... ldifFiles) throws Exception {
        if (ldifFiles == null) {
            System.out.println("Importing default data\n");
            importLdif(new LdifReader(LdapStandaloneServer.class.getResourceAsStream("/" + LDIF_FILENAME_JBOSS_ORG)));
        } else {
            for (String ldifFile : ldifFiles) {
                System.out.println("Importing " + ldifFile + "\n");
                importLdif(new LdifReader(ldifFile));
            }
        }
    }

    private void importLdif(LdifReader ldifReader) throws Exception {
        try {
            for (LdifEntry ldifEntry : ldifReader) {
                checkPartition(ldifEntry);
                System.out.print(ldifEntry.toString());
                directoryService.getAdminSession()
                        .add(new DefaultEntry(directoryService.getSchemaManager(), ldifEntry.getEntry()));
            }
        } finally {
            IOUtils.closeQuietly(ldifReader);
        }
    }

    private void checkPartition(LdifEntry ldifEntry) throws Exception {
        Dn dn = ldifEntry.getDn();
        Dn parent = dn.getParent();
        try {
            directoryService.getAdminSession().exists(parent);
        } catch (Exception e) {
            System.out.println("Creating new partition for DN=" + dn + "\n");
            AvlPartition partition = new AvlPartition(directoryService.getSchemaManager());
            partition.setId(dn.getName());
            partition.setSuffixDn(dn);
            directoryService.addPartition(partition);
        }
    }

    private String formatPossibleIpv6(String host) {
        return (host != null && host.contains(":")) ? "[" + host + "]" : host;
    }

}
