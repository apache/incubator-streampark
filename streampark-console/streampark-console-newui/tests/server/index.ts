/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Koa from 'koa';
import path from 'path';
import Router from 'koa-router';
import body from 'koa-body';
import cors from 'koa2-cors';
import koaStatic from 'koa-static';
import websockify from 'koa-websocket';
import route from 'koa-route';

import AppRoutes from './routes';

const PORT = 3300;

const app = websockify(new Koa());

app.ws.use(function (ctx, next) {
  ctx.websocket.send('connection succeeded!');
  return next(ctx);
});

app.ws.use(
  route.all('/test', function (ctx) {
    // ctx.websocket.send('Hello World');
    ctx.websocket.on('message', function (message) {
      // do something with the message from client

      if (message !== 'ping') {
        const data = JSON.stringify({
          id: Math.ceil(Math.random() * 1000),
          time: new Date().getTime(),
          res: `${message}`,
        });
        ctx.websocket.send(data);
      }
      console.log(message);
    });
  }),
);

const router = new Router();

// router
AppRoutes.forEach((route) => router[route.method](route.path, route.action));

app.use(cors());
app.use(
  body({
    encoding: 'gzip',
    multipart: true,
    formidable: {
      // uploadDir: path.join(__dirname, '/upload/'), // Set file upload directory
      keepExtensions: true,
      maxFieldsSize: 20 * 1024 * 1024,
    },
  }),
);
app.use(router.routes());
app.use(router.allowedMethods());
app.use(koaStatic(path.join(__dirname)));

app.listen(PORT, () => {
  console.log(`Application started successfully: http://localhost:${PORT}`);
});
