BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.eventos_reporteria
    ADD COLUMN IF NOT EXISTS productos_venta_json text;

COMMIT;
