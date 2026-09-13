export const ingredients = ['Trứng', 'Cà chua', 'Rau xanh', 'Thịt gà', 'Mì Ý', 'Cơm'] as const
export type Ingredient = (typeof ingredients)[number]

export interface SampleRecipe {
  id: string
  title: string
  description: string
  image: string
  minutes: number
  category: 'Món chay' | 'Giàu đạm'
  ingredients: Ingredient[]
  amounts: string[]
  steps: string[]
}

export const sampleRecipes: SampleRecipe[] = [
  {
    id: 'salad',
    title: 'Salad rau xanh tươi mát',
    description: 'Giòn mát, nhẹ bụng, trọn vị rau tươi.',
    image: '/assets/landing/salad.jpg',
    minutes: 15,
    category: 'Món chay',
    ingredients: ['Rau xanh', 'Cà chua'],
    amounts: [
      '150 g rau xà lách',
      '8 quả cà chua bi',
      '1/2 quả dưa leo',
      '1 thìa dầu ô liu',
      '1 thìa nước cốt chanh, muối và tiêu',
    ],
    steps: [
      'Rửa sạch rau, cà chua và dưa leo. Để ráo, cắt thành miếng vừa ăn.',
      'Khuấy đều dầu ô liu, nước cốt chanh, một ít muối và tiêu.',
      'Trộn nhẹ rau với nước sốt ngay trước khi dùng.',
    ],
  },
  {
    id: 'pasta',
    title: 'Mì Ý sốt cà chua',
    description: 'Một chút cà chua, một bữa ngon lành.',
    image: '/assets/landing/pasta.jpg',
    minutes: 25,
    category: 'Món chay',
    ingredients: ['Mì Ý', 'Cà chua'],
    amounts: [
      '160 g mì Ý',
      '3 quả cà chua chín',
      '2 tép tỏi',
      '1 thìa dầu ô liu',
      'Húng quế, muối và tiêu',
    ],
    steps: [
      'Luộc mì trong nước có muối theo thời gian trên bao bì. Giữ lại một ít nước luộc.',
      'Phi thơm tỏi với dầu ô liu, thêm cà chua băm và nấu nhỏ lửa khoảng 10 phút.',
      'Cho mì vào sốt, thêm chút nước luộc để sốt bám đều. Nêm vừa ăn và thêm húng quế.',
    ],
  },
  {
    id: 'chicken',
    title: 'Cơm gà áp chảo rau xanh',
    description: 'Bữa cơm đủ đầy từ những điều giản đơn.',
    image: '/assets/landing/hero-warm.webp',
    minutes: 30,
    category: 'Giàu đạm',
    ingredients: ['Thịt gà', 'Cơm', 'Rau xanh', 'Cà chua'],
    amounts: [
      '250 g ức gà',
      '2 phần cơm chín',
      '100 g rau xanh',
      '6 quả cà chua bi',
      'Dầu ăn, muối, tiêu và nước cốt chanh',
    ],
    steps: [
      'Ướp gà với muối, tiêu và ít dầu ăn khoảng 10 phút.',
      'Áp chảo gà trên lửa vừa, lật đều đến khi chín hoàn toàn, nhiệt độ tâm đạt 74°C.',
      'Xếp cơm, rau đã rửa sạch và cà chua vào bát. Thái gà, đặt lên trên và thêm nước cốt chanh theo khẩu vị.',
    ],
  },
  {
    id: 'eggs',
    title: 'Trứng xào cà chua',
    description: 'Món quen, nguyên liệu ít, cơm nhà thật ngon.',
    image: '/assets/landing/eggs.jpg',
    minutes: 10,
    category: 'Giàu đạm',
    ingredients: ['Trứng', 'Cà chua'],
    amounts: ['3 quả trứng', '2 quả cà chua', '1 nhánh hành lá', '1 thìa dầu ăn', 'Muối và tiêu'],
    steps: [
      'Đánh trứng với một ít muối. Cắt cà chua thành miếng nhỏ.',
      'Làm nóng dầu, cho trứng vào đảo đến khi vừa đông, lấy ra đĩa.',
      'Xào cà chua đến khi mềm, cho trứng trở lại và đảo đến khi chín hoàn toàn. Thêm hành lá.',
    ],
  },
]
