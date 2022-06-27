import { connectToParent } from 'penpal'

const stack = []
let callSender = null
let connectPromise = null

export function getSender() {
  if (callSender) {
    return Promise.resolve(callSender)
  }
  if (connectPromise) {
    return new Promise((resolve, reject) => {
      stack.push({ resolve, reject })
    })
  }

  connectPromise = connectToParent().promise

  return connectPromise.then((parent) => {
    callSender = parent

    while(stack.length) {
      stack.shift().resolve(callSender)
    }

    connectPromise = null

    return callSender
  }).catch(error => {
    while(stack.length) {
      stack.shift().reject(error)
    }

    connectPromise = null

    return Promise.reject(error)
  })
}
