-- Demo login: demo@taskflow.dev / Password123!  (same password for asha@taskflow.dev)
INSERT INTO users (name, email, password_hash)
VALUES ('Demo User',     'demo@taskflow.dev', '$2a$10$xpqNZPtPF/YZMwq.BfK.6eLe6whsMZ3Y9WvWZAllFzldtjRs2TY02'),
       ('Asha Teammate', 'asha@taskflow.dev', '$2a$10$xpqNZPtPF/YZMwq.BfK.6eLe6whsMZ3Y9WvWZAllFzldtjRs2TY02');

INSERT INTO projects (name, description, owner_id)
VALUES ('Website Relaunch', 'Q4 marketing site rebuild',
        (SELECT id FROM users WHERE email = 'demo@taskflow.dev'));

INSERT INTO tasks (title, description, status, priority, due_date, assignee_id, project_id)
SELECT t.title, t.descr, t.status, t.priority, CURRENT_DATE + t.offset_days,
       (SELECT id FROM users WHERE email = t.assignee), p.id
FROM projects p,
     (VALUES ('Design homepage mockups',   'Hero and pricing sections', 'IN_PROGRESS', 'HIGH',    3, 'demo@taskflow.dev'),
             ('Write copy for About page', NULL,                        'TODO',        'MEDIUM',  7, 'asha@taskflow.dev'),
             ('Fix broken footer links',   'Reported by QA',            'TODO',        'HIGH',   -2, NULL),
             ('Set up analytics',          NULL,                        'DONE',        'LOW',    -5, 'demo@taskflow.dev'))
     AS t(title, descr, status, priority, offset_days, assignee)
WHERE p.name = 'Website Relaunch';
