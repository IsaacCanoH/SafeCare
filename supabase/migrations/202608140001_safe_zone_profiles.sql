-- Una zona segura puede corresponder a muchos perfiles y un perfil a muchas zonas.
-- Los identificadores se detectan desde las tablas existentes: algunos proyectos los
-- almacenan como text y otros como uuid.

DO $$
DECLARE
    zone_id_type text;
    profile_id_type text;
BEGIN
    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO zone_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."ZonaSegura"'::regclass
      AND attribute.attname = 'idZona'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO profile_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."PerfilMonitoreado"'::regclass
      AND attribute.attname = 'idPerfil'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    IF zone_id_type IS NULL OR profile_id_type IS NULL THEN
        RAISE EXCEPTION 'No se encontraron las columnas ZonaSegura.idZona o PerfilMonitoreado.idPerfil';
    END IF;

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS public."ZonaSeguraPerfil" (
            "idZona" %1$s NOT NULL REFERENCES public."ZonaSegura"("idZona") ON DELETE CASCADE,
            "idPerfil" %2$s NOT NULL REFERENCES public."PerfilMonitoreado"("idPerfil") ON DELETE CASCADE,
            PRIMARY KEY ("idZona", "idPerfil")
        )',
        zone_id_type,
        profile_id_type
    );
END;
$$;

CREATE INDEX IF NOT EXISTS "idx_ZonaSeguraPerfil_idPerfil"
    ON public."ZonaSeguraPerfil" ("idPerfil");

-- Conserva todas las zonas que ya estaban asignadas a un solo perfil.
INSERT INTO public."ZonaSeguraPerfil" ("idZona", "idPerfil")
SELECT "idZona", "idPerfil"
FROM public."ZonaSegura"
ON CONFLICT ("idZona", "idPerfil") DO NOTHING;

ALTER TABLE public."ZonaSeguraPerfil" ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Los cuidadores consultan sus asignaciones de zona" ON public."ZonaSeguraPerfil";
CREATE POLICY "Los cuidadores consultan sus asignaciones de zona"
ON public."ZonaSeguraPerfil"
FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public."PerfilMonitoreado" profile
        WHERE profile."idPerfil" = "ZonaSeguraPerfil"."idPerfil"
          AND profile."idCuidador"::text = auth.uid()::text
    )
);

GRANT SELECT ON public."ZonaSeguraPerfil" TO authenticated;

-- Si se elimina el perfil propietario de una zona compartida, se conserva la zona
-- y se usa otro perfil asignado como referencia de compatibilidad.
CREATE OR REPLACE FUNCTION public.reassign_safe_zones_before_profile_delete()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    WITH replacements AS (
        SELECT DISTINCT ON (zone."idZona")
            zone."idZona",
            assignment."idPerfil"
        FROM public."ZonaSegura" zone
        JOIN public."ZonaSeguraPerfil" assignment
            ON assignment."idZona" = zone."idZona"
        WHERE zone."idPerfil" = OLD."idPerfil"
          AND assignment."idPerfil" <> OLD."idPerfil"
        ORDER BY zone."idZona", assignment."idPerfil"
    )
    UPDATE public."ZonaSegura" zone
    SET "idPerfil" = replacements."idPerfil"
    FROM replacements
    WHERE zone."idZona" = replacements."idZona";

    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS reassign_safe_zones_before_profile_delete
    ON public."PerfilMonitoreado";
CREATE TRIGGER reassign_safe_zones_before_profile_delete
BEFORE DELETE ON public."PerfilMonitoreado"
FOR EACH ROW
EXECUTE FUNCTION public.reassign_safe_zones_before_profile_delete();

-- Si se intentÃ³ una versiÃ³n anterior de esta migraciÃ³n, evita dejar funciones
-- sobrecargadas que PostgREST no pueda resolver desde los parÃ¡metros JSON.
DROP FUNCTION IF EXISTS public.create_safe_zone_with_profiles(
    uuid, text, double precision, double precision, double precision, uuid[]
);
DROP FUNCTION IF EXISTS public.update_safe_zone_with_profiles(
    uuid, text, double precision, double precision, double precision, uuid[]
);

CREATE OR REPLACE FUNCTION public.create_safe_zone_with_profiles(
    p_id_zona text,
    p_nombre text,
    p_latitud double precision,
    p_longitud double precision,
    p_radio double precision,
    p_id_perfiles text[]
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    zone_id_type text;
    profile_id_type text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debes iniciar sesiÃ³n para crear una zona segura';
    END IF;

    IF COALESCE(cardinality(p_id_perfiles), 0) = 0 THEN
        RAISE EXCEPTION 'Selecciona al menos un perfil';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM unnest(p_id_perfiles) AS selected("idPerfil")
        LEFT JOIN public."PerfilMonitoreado" profile
            ON profile."idPerfil"::text = selected."idPerfil"
        WHERE profile."idPerfil" IS NULL
           OR profile."idCuidador"::text <> auth.uid()::text
    ) THEN
        RAISE EXCEPTION 'No puedes asignar zonas a perfiles de otro cuidador';
    END IF;

    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO zone_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."ZonaSegura"'::regclass
      AND attribute.attname = 'idZona'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO profile_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."PerfilMonitoreado"'::regclass
      AND attribute.attname = 'idPerfil'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    EXECUTE format(
        'INSERT INTO public."ZonaSegura" (
            "idZona", "nombre", "latitudCentro", "longitudCentro", "radioMetros", "activa", "idPerfil"
        ) VALUES ($1::%1$s, $2, $3, $4, $5, true, ($6)[1]::%2$s)',
        zone_id_type,
        profile_id_type
    ) USING p_id_zona, p_nombre, p_latitud, p_longitud, p_radio, p_id_perfiles;

    EXECUTE format(
        'INSERT INTO public."ZonaSeguraPerfil" ("idZona", "idPerfil")
         SELECT $1::%1$s, profile."idPerfil"
         FROM public."PerfilMonitoreado" profile
         WHERE profile."idPerfil"::text = ANY($2::text[])',
        zone_id_type
    ) USING p_id_zona, p_id_perfiles;
END;
$$;

CREATE OR REPLACE FUNCTION public.update_safe_zone_with_profiles(
    p_id_zona text,
    p_nombre text,
    p_latitud double precision,
    p_longitud double precision,
    p_radio double precision,
    p_id_perfiles text[]
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    zone_id_type text;
    profile_id_type text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debes iniciar sesiÃ³n para actualizar una zona segura';
    END IF;

    IF COALESCE(cardinality(p_id_perfiles), 0) = 0 THEN
        RAISE EXCEPTION 'Selecciona al menos un perfil';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM public."ZonaSeguraPerfil" assignment
        JOIN public."PerfilMonitoreado" profile
            ON profile."idPerfil" = assignment."idPerfil"
        WHERE assignment."idZona"::text = p_id_zona
          AND profile."idCuidador"::text = auth.uid()::text
    ) THEN
        RAISE EXCEPTION 'No tienes permiso para actualizar esta zona';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM unnest(p_id_perfiles) AS selected("idPerfil")
        LEFT JOIN public."PerfilMonitoreado" profile
            ON profile."idPerfil"::text = selected."idPerfil"
        WHERE profile."idPerfil" IS NULL
           OR profile."idCuidador"::text <> auth.uid()::text
    ) THEN
        RAISE EXCEPTION 'No puedes asignar zonas a perfiles de otro cuidador';
    END IF;

    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO zone_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."ZonaSegura"'::regclass
      AND attribute.attname = 'idZona'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    SELECT pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
    INTO profile_id_type
    FROM pg_catalog.pg_attribute attribute
    WHERE attribute.attrelid = 'public."PerfilMonitoreado"'::regclass
      AND attribute.attname = 'idPerfil'
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    EXECUTE format(
        'UPDATE public."ZonaSegura"
         SET "nombre" = $2,
             "latitudCentro" = $3,
             "longitudCentro" = $4,
             "radioMetros" = $5,
             "idPerfil" = ($6)[1]::%1$s
         WHERE "idZona" = $1::%2$s',
        profile_id_type,
        zone_id_type
    ) USING p_id_zona, p_nombre, p_latitud, p_longitud, p_radio, p_id_perfiles;

    EXECUTE format(
        'DELETE FROM public."ZonaSeguraPerfil" WHERE "idZona" = $1::%s',
        zone_id_type
    ) USING p_id_zona;

    EXECUTE format(
        'INSERT INTO public."ZonaSeguraPerfil" ("idZona", "idPerfil")
         SELECT $1::%1$s, profile."idPerfil"
         FROM public."PerfilMonitoreado" profile
         WHERE profile."idPerfil"::text = ANY($2::text[])',
        zone_id_type
    ) USING p_id_zona, p_id_perfiles;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_safe_zone_with_profiles(text, text, double precision, double precision, double precision, text[])
    TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_safe_zone_with_profiles(text, text, double precision, double precision, double precision, text[])
    TO authenticated;

NOTIFY pgrst, 'reload schema';
