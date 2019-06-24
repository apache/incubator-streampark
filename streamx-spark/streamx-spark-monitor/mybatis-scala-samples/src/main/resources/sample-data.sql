--
--    Copyright 2011-2015 the original author or authors.
--
--    Licensed under the Apache License, Version 2.0 (the "License");
--    you may not use this file except in compliance with the License.
--    You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.
--


INSERT INTO people_group (id_, name_)
    VALUES
        (1, 'Customers'),
        (2, 'Suppliers'),
        (3, 'Employees');

INSERT INTO person (id_, first_name_, last_name_, group_id_)
    VALUES
        (1, 'Brenda', 'Gates', 1),
        (2, 'Sara', 'Jobs', 2),
        (3, 'Janeth', 'Gosling', 3),
        (4, 'John', 'Doe', 1),
        (5, 'Mary', 'Jackson', 2);

INSERT INTO contact_info (owner_id_, street_address_, phone_number_)
    VALUES
        (1, '637 St Monica', '555-5647809'),
        (2, '23 Wall St',    '555-0485959'),
        (2, '78 Road X',     '554-8899888'),
        (3, '567 Kong St',   '555-0989988'),
        (4, '5 Springfield', '555-0909090'),
        (5, 'Another place', '555-6978799');

-- Postgresql only: update sequences
SELECT setval('people_group_id__seq', 4);
SELECT setval('person_id__seq', 6);
