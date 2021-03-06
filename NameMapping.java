package com.example;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

import java.util.Locale;

public class NameMapping extends ImplicitNamingStrategyJpaCompliantImpl {
    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        String name = transformAttributePath( source.getAttributePath() );
        name = addUnderscores(name);
        return toIdentifier( name, source.getBuildingContext() );
    }

    protected static String addUnderscores(String name) {
        StringBuilder buf = new StringBuilder(name.replace('.', '_') );
        for (int i=1; i<buf.length()-1; i++) {
            if (
                    Character.isLowerCase( buf.charAt(i-1) ) &&
                            Character.isUpperCase(buf.charAt(i)) &&
                            Character.isLowerCase(buf.charAt(i+1))
            ) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase(Locale.ROOT);
    }

}
