CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  action_url VARCHAR(255),
  source_inventory_item_id BIGINT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_notifications_dedup
  ON notifications (user_id, source_inventory_item_id, type);

CREATE INDEX idx_notifications_user_unread
  ON notifications (user_id, is_read);
