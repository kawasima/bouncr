package net.unit8.bouncr;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.tools.schemaframework.IndexDefinition;

import java.sql.SQLException;

public class CaseConvertCustomizer implements SessionCustomizer {
    private String convertToUnderscore(String name) {
        if (name.equalsIgnoreCase("begintime")) {
            System.err.println();
        }
        StringBuffer buf = new StringBuffer(name.replace('.', '_'));
        for (int i = 1; i < buf.length() - 1; i++) {
            if (Character.isLowerCase(buf.charAt(i - 1)) && Character.isUpperCase(buf.charAt(i)) && Character.isLowerCase(buf.charAt(i + 1))) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase();
    }

    @Override
    public void customize(Session session) throws SQLException {
        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            //Only change the table name for non-embedable entities with no @Table already
            if (!descriptor.getTables().isEmpty() && descriptor.getAlias().equalsIgnoreCase(descriptor.getTableName())) {
                String tableName = convertToUnderscore(descriptor.getTableName());
                descriptor.setTableName(tableName);
                for (IndexDefinition index : descriptor.getTables().get(0).getIndexes()) {
                    index.setTargetTable(tableName);
                }
            }
            for (DatabaseMapping mapping : descriptor.getMappings()) {
                // Only change the column name for non-embedable entities with
                // no @Column already

                if (mapping instanceof AggregateObjectMapping) {
                    for (Association association : ((AggregateObjectMapping) mapping).getAggregateToSourceFieldAssociations()) {
                        DatabaseField field = (DatabaseField) association.getValue();
                        field.setName(convertToUnderscore(field.getName()));

                        for (DatabaseMapping attrMapping : session.getDescriptor(((AggregateObjectMapping) mapping).getReferenceClass()).getMappings()) {
                            if (attrMapping.getAttributeName().equalsIgnoreCase((String) association.getKey())) {
                                ((AggregateObjectMapping) mapping).addFieldTranslation(field, convertToUnderscore(attrMapping.getAttributeName()));
                                ((AggregateObjectMapping) mapping).getAggregateToSourceFields().remove(association.getKey());
                                break;
                            }
                        }
                    }
                } else if (mapping instanceof ObjectReferenceMapping) {
                    for (DatabaseField foreignKey : ((ObjectReferenceMapping) mapping).getForeignKeyFields()) {
                        foreignKey.setName(convertToUnderscore(foreignKey.getName()));
                    }
                } else if (mapping instanceof DirectMapMapping) {
                    for (DatabaseField referenceKey : ((DirectMapMapping) mapping).getReferenceKeyFields()) {
                        referenceKey.setName(convertToUnderscore(referenceKey.getName()));
                    }
                    for (DatabaseField sourceKey : ((DirectMapMapping) mapping).getSourceKeyFields()) {
                        sourceKey.setName(convertToUnderscore(sourceKey.getName()));
                    }
                } else {
                    DatabaseField field = mapping.getField();
                    if (field != null && !mapping.getAttributeName().isEmpty() && field.getName().equalsIgnoreCase(mapping.getAttributeName())) {
                        field.setName(convertToUnderscore(mapping.getAttributeName()));
                    }
                }
            }
        }
    }
}
