-- Una zona segura puede corresponder a muchos perfiles y un perfil a muchas zonas.
-- Se mantiene ZonaSegura.idPerfil para conservar compatibilidad con instalaciones anteriores;
-- la fuente de verdad para las asignaciones es ZonaSeguraPerfil.

CREATE TABLE IF NOT EXISTS public."ZonaSeguraPerfil" (
    "idZona" uuid NOT NULL REFERENCES public."ZonaSegura"("idZona") ON DELETE CASCADE,
    "idPerfil" uuid NOT NULL REFERENCES public."PerfilMonitoreado"("idPerfil") ON DELETE CASCADE,
    PRIMARY KEY ("idZona", "idPerfil")
);

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
        FROM public."PerfilMonitoreado" perfil
        WHERE perfil."idPerfil" = "ZonaSeguraPerfil"."idPerfil"
          AND perfil."idCuidador" = auth.uid()
    )
);

GRANT SELECT ON public."ZonaSeguraPerfil" TO authenticated;

-- Si se elimina el perfil que antes fungÃ­a como propietario de una zona compartida,
-- se conserva la zona y se usa otro perfil asignado como referencia de compatibilidad.
CREATE OR REPLACE FUNCTION public.reassign_safe_zones_before_profile_delete()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    WITH reemplazos AS (
        SELECT DISTINCT ON (zona."idZona")
            zona."idZona",
            asignacion."idPerfil"
        FROM public."ZonaSegura" zona
        JOIN public."ZonaSeguraPerfil" asignacion
            ON asignacion."idZona" = zona."idZona"
        WHERE zona."idPerfil" = OLD."idPerfil"
          AND asignacion."idPerfil" <> OLD."idPerfil"
        ORDER BY zona."idZona", asignacion."idPerfil"
    )
    UPDATE public."ZonaSegura" zona
    SET "idPerfil" = reemplazos."idPerfil"
    FROM reemplazos
    WHERE zona."idZona" = reemplazos."idZona";

    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS reassign_safe_zones_before_profile_delete
    ON public."PerfilMonitoreado";
CREATE TRIGGER reassign_safe_zones_before_profile_delete
BEFORE DELETE ON public."PerfilMonitoreado"
FOR EACH ROW
EXECUTE FUNCTION public.reassign_safe_zones_before_profile_delete();

CREATE OR REPLACE FUNCTION public.create_safe_zone_with_profiles(
    p_id_zona uuid,
    p_nombre text,
    p_latitud double precision,
    p_longitud double precision,
    p_radio double precision,
    p_id_perfiles uuid[]
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debes iniciar sesiÃ³n para crear una zona segura';
    END IF;

    IF COALESCE(cardinality(p_id_perfiles), 0) = 0 THEN
        RAISE EXCEPTION 'Selecciona al menos un perfil';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM unnest(p_id_perfiles) AS seleccionado("idPerfil")
        LEFT JOIN public."PerfilMonitoreado" perfil
            ON perfil."idPerfil" = seleccionado."idPerfil"
        WHERE perfil."idPerfil" IS NULL
           OR perfil."idCuidador" <> auth.uid()
    ) THEN
        RAISE EXCEPTION 'No puedes asignar zonas a perfiles de otro cuidador';
    END IF;

    INSERT INTO public."ZonaSegura" (
        "idZona", "nombre", "latitudCentro", "longitudCentro", "radioMetros", "activa", "idPerfil"
    ) VALUES (
        p_id_zona, p_nombre, p_latitud, p_longitud, p_radio, true, p_id_perfiles[1]
    );

    INSERT INTO public."ZonaSeguraPerfil" ("idZona", "idPerfil")
    SELECT p_id_zona, DISTINCT_PROFILE."idPerfil"
    FROM (
        SELECT DISTINCT "idPerfil" FROM unnest(p_id_perfiles) AS perfiles("idPerfil")
    ) AS DISTINCT_PROFILE;
END;
$$;

CREATE OR REPLACE FUNCTION public.update_safe_zone_with_profiles(
    p_id_zona uuid,
    p_nombre text,
    p_latitud double precision,
    p_longitud double precision,
    p_radio double precision,
    p_id_perfiles uuid[]
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debes iniciar sesiÃ³n para actualizar una zona segura';
    END IF;

    IF COALESCE(cardinality(p_id_perfiles), 0) = 0 THEN
        RAISE EXCEPTION 'Selecciona al menos un perfil';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM public."ZonaSeguraPerfil" asignacion
        JOIN public."PerfilMonitoreado" perfil
            ON perfil."idPerfil" = asignacion."idPerfil"
        WHERE asignacion."idZona" = p_id_zona
          AND perfil."idCuidador" = auth.uid()
    ) THEN
        RAISE EXCEPTION 'No tienes permiso para actualizar esta zona';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM unnest(p_id_perfiles) AS seleccionado("idPerfil")
        LEFT JOIN public."PerfilMonitoreado" perfil
            ON perfil."idPerfil" = seleccionado."idPerfil"
        WHERE perfil."idPerfil" IS NULL
           OR perfil."idCuidador" <> auth.uid()
    ) THEN
        RAISE EXCEPTION 'No puedes asignar zonas a perfiles de otro cuidador';
    END IF;

    UPDATE public."ZonaSegura"
    SET "nombre" = p_nombre,
        "latitudCentro" = p_latitud,
        "longitudCentro" = p_longitud,
        "radioMetros" = p_radio,
        "idPerfil" = p_id_perfiles[1]
    WHERE "idZona" = p_id_zona;

    DELETE FROM public."ZonaSeguraPerfil"
    WHERE "idZona" = p_id_zona;

    INSERT INTO public."ZonaSeguraPerfil" ("idZona", "idPerfil")
    SELECT p_id_zona, DISTINCT_PROFILE."idPerfil"
    FROM (
        SELECT DISTINCT "idPerfil" FROM unnest(p_id_perfiles) AS perfiles("idPerfil")
    ) AS DISTINCT_PROFILE;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_safe_zone_with_profiles(uuid, text, double precision, double precision, double precision, uuid[])
    TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_safe_zone_with_profiles(uuid, text, double precision, double precision, double precision, uuid[])
    TO authenticated;
