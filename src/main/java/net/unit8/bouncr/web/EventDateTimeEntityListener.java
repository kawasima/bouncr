package net.unit8.bouncr.web;

import org.seasar.doma.jdbc.entity.EntityListener;
import org.seasar.doma.jdbc.entity.PreInsertContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static enkan.util.ReflectionUtils.tryReflection;

public class EventDateTimeEntityListener<ENTITY> implements EntityListener<ENTITY> {
    public void preInsert(ENTITY entity, PreInsertContext<ENTITY> context) {
        if (entity == null)
            return;

        Stream.of(context.getEntityType().getEntityClass().getDeclaredFields())
                .filter(f -> f.getAnnotation(EventDateTime.class) != null)
                .forEach(f -> tryReflection(() -> {
                    Class<?> fieldType = f.getType();
                    f.setAccessible(true);
                    if (fieldType.equals(LocalDateTime.class)) {
                        f.set(entity, LocalDateTime.now());
                    } else if (fieldType.equals(Timestamp.class)){
                        f.set(entity, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
                    }

                    return null;
                }));
    }

}
