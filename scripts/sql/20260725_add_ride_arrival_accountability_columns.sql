-- Adds arrival accountability fields used by the cab handoff unlock flow.
-- Run once before deploying code that validates the updated PbRideEntity schema.

ALTER TABLE pb_ride
    ADD COLUMN primary_arrived_at DATETIME(6) NULL,
    ADD COLUMN secondary_arrived_at DATETIME(6) NULL,
    ADD COLUMN primary_arrival_lat DOUBLE NULL,
    ADD COLUMN primary_arrival_lng DOUBLE NULL,
    ADD COLUMN primary_arrival_accuracy_meters DOUBLE NULL,
    ADD COLUMN primary_arrival_distance_km DOUBLE NULL,
    ADD COLUMN secondary_arrival_lat DOUBLE NULL,
    ADD COLUMN secondary_arrival_lng DOUBLE NULL,
    ADD COLUMN secondary_arrival_accuracy_meters DOUBLE NULL,
    ADD COLUMN secondary_arrival_distance_km DOUBLE NULL;
