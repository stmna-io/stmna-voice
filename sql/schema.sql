-- STMNA Voice -- Database Schema
-- Apply to stmna_voice database:
--   podman exec -i postgres-voice psql -U voice -d stmna_voice < schema.sql

-- UUID generation for request_id
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Training pairs: raw vs LLM-cleaned transcripts for future fine-tuning
CREATE TABLE IF NOT EXISTS voice_training_pairs (
    id SERIAL PRIMARY KEY,
    audio_path TEXT,
    audio_duration_seconds DOUBLE PRECISION,
    raw_transcript TEXT NOT NULL,
    polished_transcript TEXT NOT NULL,
    final_transcript TEXT,
    language VARCHAR(10) DEFAULT 'en',
    client_type VARCHAR(50),
    whisper_model VARCHAR(100),
    qwen_model VARCHAR(100),
    whisper_duration_ms INTEGER,
    qwen_duration_ms INTEGER,
    total_duration_ms INTEGER,
    approved BOOLEAN DEFAULT FALSE,
    needs_correction BOOLEAN DEFAULT FALSE,
    manual_correction TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);

-- Latency metrics: per-request timing breakdown for performance monitoring
CREATE TABLE IF NOT EXISTS voice_latency_metrics (
    id SERIAL PRIMARY KEY,
    request_id UUID DEFAULT uuid_generate_v4(),
    client_type VARCHAR(50),
    audio_duration_seconds DOUBLE PRECISION,
    audio_file_size_bytes INTEGER,
    upload_receive_ms INTEGER,
    whisper_queue_ms INTEGER,
    whisper_inference_ms INTEGER,
    whisper_total_ms INTEGER,
    qwen_inference_ms INTEGER,
    response_format_ms INTEGER,
    total_pipeline_ms INTEGER,
    db_write_ms INTEGER,
    audio_archive_ms INTEGER,
    realtime_factor DOUBLE PRECISION,
    chars_per_second DOUBLE PRECISION,
    whisper_model VARCHAR(100),
    qwen_model VARCHAR(100),
    language VARCHAR(10) DEFAULT 'en',
    transcript_length INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_training_pairs_created ON voice_training_pairs (created_at);
CREATE INDEX IF NOT EXISTS idx_training_pairs_approved ON voice_training_pairs (approved);
CREATE INDEX IF NOT EXISTS idx_training_pairs_needs_correction ON voice_training_pairs (needs_correction);
CREATE INDEX IF NOT EXISTS idx_latency_metrics_created ON voice_latency_metrics (created_at);
CREATE INDEX IF NOT EXISTS idx_latency_metrics_request_id ON voice_latency_metrics (request_id);
