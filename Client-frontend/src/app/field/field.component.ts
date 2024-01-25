import {Component, ElementRef, HostListener, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {RxStomp} from '@stomp/rx-stomp';
import {MatButton} from '@angular/material/button';
import {Point} from '@angular/cdk/drag-drop';
import {Subscription} from 'rxjs';
import {NgClass, NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-field',
  standalone: true,
  imports: [
    MatButton,
    NgForOf,
    NgIf,
    NgClass
  ],
  templateUrl: './field.component.html',
  styleUrl: './field.component.css'
})
export class FieldComponent implements OnInit, OnDestroy {

  @Input('uid') uid: string;

  @ViewChild('player') player: ElementRef<HTMLElement>;
  @ViewChild('secondPlayer') secondPlayer: ElementRef<HTMLElement>;
  @ViewChild('ball') ball: ElementRef<HTMLElement>;
  @ViewChild('field') field: ElementRef<HTMLElement>;

  private rxStomp: RxStomp = new RxStomp();
  private playerPos: Point = {x: 0, y: 0};
  private speed = 1;
  private acceleration = 0.1;
  private subscriptions: Subscription = new Subscription();
  private keyState: { [key: string]: boolean } = {};
  private interval: number;
  score: Point = {x: 0, y: 0};
  private currentPlayerName: string;
  private hostPlayerName: string;
  private firstMessage = true;
  private playersName:Set<string>;

  constructor() {
  }

  @HostListener('window:beforeunload')
  ngOnDestroy(): void {
    this.subscriptions?.unsubscribe();
    this.rxStomp?.deactivate();
    clearInterval(this.interval);
    window.removeEventListener('keydown', (event: KeyboardEvent) => {
      this.keyState[event.key] = true;
    }, true);
    window.removeEventListener('keyup', (event: KeyboardEvent) => {
      this.keyState[event.key] = false;
    }, true);
  }

  ngOnInit(): void {
    this.currentPlayerName = localStorage.getItem('player');
    this.playersName = new Set<string>(this.currentPlayerName);

    this.rxStomp.configure({
      brokerURL: 'ws://localhost:8080/ws'
    });

    this.rxStomp.activate();
    this.subscriptions.add(
      this.rxStomp.watch({destination: `/topic/game/${this.uid}`, subHeaders: {player: this.currentPlayerName}})
        .subscribe((message) => {
          let gameObjectState: GameObjectState = JSON.parse(message.body);

          if (gameObjectState.type === GameObjectType.PLAYER && this.currentPlayerName !== gameObjectState.clientId) {
            this.playersName.add(gameObjectState.clientId);
          }

          if (this.firstMessage) {
            this.firstMessage = false;
            if (gameObjectState.clientId === this.currentPlayerName && gameObjectState.type === GameObjectType.PLAYER) {
              this.playerPos = gameObjectState.position;
              this.player.nativeElement.style.left = `${gameObjectState.position.x}%`;
              this.player.nativeElement.style.top = `${gameObjectState.position.y}%`;
            }
          }

          if (this.secondPlayerJoined) {
            if (this.secondPlayer && gameObjectState.type === GameObjectType.PLAYER && this.currentPlayerName !== gameObjectState.clientId) {
                this.secondPlayer.nativeElement.style.left = `${gameObjectState.position.x}%`;
                this.secondPlayer.nativeElement.style.top = `${gameObjectState.position.y}%`;
            }

            if (this.ball && gameObjectState.type === GameObjectType.BALL) {
                this.ball.nativeElement.style.left = `${gameObjectState.position.x}%`;
                this.ball.nativeElement.style.top = `${gameObjectState.position.y}%`;
            }
          }


          if (gameObjectState.type === GameObjectType.SCORE) {
            if (this.hostPlayerName == null) {
              this.hostPlayerName = gameObjectState.clientId;
            }
            this.score = gameObjectState.position;
          }
        })
    );

    this.setKeyPressWindowListeners();

    this.interval = setInterval(() => {
      let posChanged = false;

      if (this.isUpOrLeftKeyPressed) {
        posChanged = true;
        this.playerPos.y -= this.speed + this.acceleration;
      } else if (this.isDownOrRightKeyPressed) {
        posChanged = true;
        this.playerPos.y += this.speed + this.acceleration;
      }

      if (this.playerPos.y < 0) {
        this.playerPos.y = 0;
      } else if (this.playerPos.y > 100 - 20) {
        this.playerPos.y = 80;
      }

      if (posChanged) {
        this.rxStomp.publish({destination: `/app/game-process/${this.uid}`, body: JSON.stringify(this.playerPos)});

        this.player.nativeElement.style.left = `${this.playerPos.x}`;
        this.player.nativeElement.style.top = this.playerPos.y + '%';
      }
    }, 10);
  }

  get isUpOrLeftKeyPressed() {
    return this.keyState['ArrowUp'] || this.keyState['ArrowLeft'] || this.keyState['W'] || this.keyState['A'];
  }

  get isDownOrRightKeyPressed() {
    return this.keyState['ArrowDown'] || this.keyState['ArrowRight'] || this.keyState['S'] || this.keyState['D'];
  }

  get secondPlayerJoined(): boolean {
    return this.playersName?.size > 1;
  }

  get youAreFirstPlayer(): boolean {
    return this.currentPlayerName === this.hostPlayerName;
  }
  private setKeyPressWindowListeners(): void {
    window.addEventListener('keydown', (event: KeyboardEvent) => {
      this.keyState[event.key] = true;
    }, true);

    window.addEventListener('keyup', (event: KeyboardEvent) => {
      this.keyState[event.key] = false;
    }, true);
  }
}

interface GameObjectState {
  type: GameObjectType,
  position: Point,
  clientId: string
}

enum GameObjectType {
  BALL = "BALL",
  PLAYER = "PLAYER",
  SCORE = "SCORE"
}
